package com.simats.genetics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.toColorInt
import com.simats.genetics.network.responses.PedigreeData
import com.simats.genetics.network.responses.PedigreeLink
import com.simats.genetics.network.responses.PedigreeNode
import kotlin.comparisons.compareByDescending
import kotlin.comparisons.thenBy

class PedigreeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pedigreeData: PedigreeData? = null

    // ── Paints ──────────────────────────────────────────────────────────────
    private val colorId   = "#222222".toColorInt()
    private val colorMain = "#111111".toColorInt()
    private val colorBlue = "#1565C0".toColorInt()
    private val colorGrey = "#555555".toColorInt()
    private val colorAffected  = "#FF5252".toColorInt() // Softer red
    private val colorUnaffected = "#2E7D32".toColorInt() // Healthy green
    private val colorMale      = "#42A5F5".toColorInt()
    private val colorFemale    = "#FF80AB".toColorInt()
    private val colorCarrier   = "#FF7043".toColorInt()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorId
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorMain
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    // Node ID label (e.g. "I-1") below node
    private val nodeIdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorId
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Name label (below node ID)
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#333333".toColorInt()
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    // Proband name in blue
    private val probandNamePaint = Paint(namePaint).apply {
        color = colorBlue
        typeface = Typeface.DEFAULT_BOLD
    }

    // Relationship label
    private val relPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorGrey
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // Health status label (colored)
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // "Proband" badge label in blue
    private val probandBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBlue
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Generation label on the left (e.g. "Generation I")
    private val genLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#444444".toColorInt()
        textSize = 42f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }

    // Carrier dot
    private val carrierDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorMain
        style = Paint.Style.FILL
    }

    // Proband arrow
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBlue
        strokeWidth = 6f
        style = Paint.Style.FILL_AND_STROKE
    }

    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#AAAAAA".toColorInt()
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    // ── Layout constants ─────────────────────────────────────────────────────
    private var nodeSize      = 100f         // half-width/radius
    private var hSpacing      = 320f         // horizontal gap between node centers
    private val vSpacing      = 600f         // vertical gap between generation rows
    private val topPadding    = 180f         // space above first row
    private val leftLabelW    = 300f         // width reserved for generation labels on left
    private val sidePad       = 80f          // additional right/bottom padding
    private val sideGap       = 300f         // extra gap between family sides

    // node centre positions keyed by nodeId
    private val nodePos = mutableMapOf<String, PointF>()
    private val pointPool = mutableListOf<PointF>()
    private var poolIdx = 0

    // Caches
    private var cachedRows: Map<Int, List<PedigreeNode>>? = null
    private var cachedSortedGens: List<Int>? = null
    private val wrappedTextCache = mutableMapOf<String, List<String>>()

    // Pre-allocated drawing objects
    private val sharedPath = Path()
    private val sharedRect = RectF()
    private val sharedStringBuilder = StringBuilder()

    init {
        // Use Hardware Acceleration for performance and to avoid OOM with large canvases
        // setLayerType(LAYER_TYPE_SOFTWARE, null) // Removed to prevent OOM on large bitmaps
        
        // Removed setShadowLayer as it requires software layer or causes issues on large canvases
        
        // Pre-fill pool
        repeat(100) { pointPool.add(PointF()) }
    }

    private fun getPoint(x: Float, y: Float): PointF {
        if (poolIdx >= pointPool.size) pointPool.add(PointF())
        return pointPool[poolIdx++].apply { set(x, y) }
    }

    fun setData(data: PedigreeData) {
        Log.d("PedigreeChart", "setData: ${data.nodes.size} nodes, ${data.links.size} links")
        pedigreeData = data
        // Reset caches
        cachedRows = buildRows(data.nodes)
        cachedSortedGens = cachedRows?.keys?.sorted()
        wrappedTextCache.clear()
        
        Log.d("PedigreeChart", "Rows built: ${cachedRows?.keys}")
        requestLayout()
        invalidate()
    }

    // ── Measure ──────────────────────────────────────────────────────────────
    private fun getHSpacing(gen: Int, maxCols: Int): Float {
        // Grandparents (Gen 0) need more space for long labels like "Grandfather (Mother Side)"
        if (gen == 0) return 450f
        
        return when {
            maxCols > 10 -> 220f
            maxCols > 5  -> 260f
            else         -> 320f
        }
    }

    private fun calculateRowWidth(nodes: List<PedigreeNode>, hSpace: Float): Float {
        if (nodes.isEmpty()) return 0f
        var totalW = 0f
        var prevSide: String? = null
        nodes.forEachIndexed { i, node ->
            if (i > 0) {
                totalW += hSpace
                val currentSide = node.side_of_family ?: "None"
                if (prevSide != null && prevSide != currentSide && currentSide != "None") {
                    totalW += sideGap
                }
            }
            prevSide = node.side_of_family ?: "None"
        }
        return totalW
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            val d = pedigreeData
            if (d == null || d.nodes.isEmpty()) {
                setMeasuredDimension(800, 600)
                return
            }
            
            if (cachedRows == null || cachedSortedGens == null) {
                cachedRows = buildRows(d.nodes)
                cachedSortedGens = cachedRows?.keys?.sorted()
            }

            val rowsMap = cachedRows ?: emptyMap()
            val sortedIndices = cachedSortedGens ?: emptyList()
            val maxCols = rowsMap.values.maxOfOrNull { it.size } ?: 1

            // Calculate max width across all rows, accounting for sideGap and generation-specific spacing
            var maxRowW = 0f
            sortedIndices.forEach { gen ->
                val nodes = rowsMap[gen] ?: return@forEach
                val hSpace = getHSpacing(gen, maxCols)
                val rowW = calculateRowWidth(nodes, hSpace)
                if (rowW > maxRowW) maxRowW = rowW
            }

            val w = (leftLabelW + sidePad + maxRowW + nodeSize + sidePad).toInt().coerceAtMost(5000)
            val h = (topPadding + (sortedIndices.size - 1).coerceAtLeast(0) * vSpacing + nodeSize + 400f + sidePad).toInt().coerceAtMost(5000)

            setMeasuredDimension(
                resolveSize(w.coerceAtLeast(800), widthMeasureSpec),
                resolveSize(h.coerceAtLeast(600), heightMeasureSpec)
            )
        } catch (e: Exception) {
            Log.e("PedigreeChart", "onMeasure failed", e)
            setMeasuredDimension(800, 600)
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw a light background color to confirm the view IS being drawn (no "white screen")
        canvas.drawColor("#F0F2F5".toColorInt())

        try {
            val d = pedigreeData

            // Placeholder when no data
            if (d == null || d.nodes.isEmpty()) {
                canvas.drawText("No pedigree data available", width / 2f, height / 2f, placeholderPaint)
                return
            }

            // Ensure caches are populated
            if (cachedRows == null || cachedSortedGens == null) {
                cachedRows = buildRows(d.nodes)
                cachedSortedGens = cachedRows?.keys?.sorted()
            }

            nodePos.clear()
            poolIdx = 0
            val rowsMap = cachedRows ?: return
            val sortedGenIndices = cachedSortedGens ?: return

            // 1. Compute centre positions and draw generation labels
            val maxCols = rowsMap.values.maxOfOrNull { it.size } ?: 1
            
            sortedGenIndices.forEachIndexed { displayRowIdx, actualGenIndex ->
                val cy = topPadding + displayRowIdx * vSpacing
                val rowNodes = rowsMap[actualGenIndex]!!
                val hSpace = getHSpacing(actualGenIndex, maxCols)
                
                // Calculate row width using shared logic
                val totalW = calculateRowWidth(rowNodes, hSpace)
                
                // Centering logic
                val chartAreaW = width - (leftLabelW + sidePad*2)
                var currentX = leftLabelW + sidePad + (chartAreaW - totalW).coerceAtLeast(0f) / 2f
                
                val genLabel = "GENERATION ${toRoman(actualGenIndex + 1)}"
                canvas.drawText(genLabel, 40f, cy + 10f, genLabelPaint)
                
                var prevSide: String? = null
                rowNodes.forEachIndexed { i, node ->
                    val currentSide = node.side_of_family ?: "None"
                    if (i > 0) {
                        currentX += hSpace
                        if (prevSide != null && prevSide != currentSide && currentSide != "None") {
                            currentX += sideGap
                        }
                    }
                    nodePos[node.nodeId] = getPoint(currentX, cy)
                    prevSide = currentSide
                }
            }

            // 2. Draw relationship links
            try {
                drawLinks(canvas, d)
            } catch (e: Exception) {
                Log.e("PedigreeChart", "Error drawing links: ${e.message}")
            }

            // 3. Draw nodes (shapes + labels)
            sortedGenIndices.forEach { actualGenIndex ->
                val rowNodes = rowsMap[actualGenIndex]!!
                rowNodes.forEachIndexed { colIdx, node ->
                    val p = nodePos[node.nodeId] ?: return@forEachIndexed
                    val nodeLabel = "${toRoman(actualGenIndex + 1)}-${colIdx + 1}"
                    try {
                        drawNode(canvas, node, p.x, p.y, nodeLabel)
                    } catch (e: Exception) {
                        Log.e("PedigreeChart", "Error drawing node ${node.nodeId}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PedigreeChart", "CRITICAL DRAW ERROR", e)
            val errPaint = Paint().apply { color = Color.RED; textSize = 40f; textAlign = Paint.Align.CENTER }
            canvas.drawText("Drawing Error: ${e.localizedMessage}", width/2f, 100f, errPaint)
        }
    }

    // ── Link drawing ─────────────────────────────────────────────────────────
    private fun drawLinks(canvas: Canvas, d: PedigreeData) {
        val half = nodeSize / 2f
        val effectiveLinks = d.links.toMutableList()

        // Synthesize links if they are missing (common for Ramesh and similar patients)
        if (effectiveLinks.isEmpty()) {
            val rows = cachedRows ?: return
            
            // Synthesis: Grandparents (Gen 0) -> Parents (Gen 1)
            synthesizeLinks(rows, 0, 1, effectiveLinks)
            // Synthesis: Parents (Gen 1) -> Proband/Siblings (Gen 2)
            synthesizeLinks(rows, 1, 2, effectiveLinks)
            // Synthesis: Proband/Spouse (Gen 2) -> Children (Gen 3)
            synthesizeLinks(rows, 2, 3, effectiveLinks)
            // Synthesis: Children (Gen 3) -> Grandchildren (Gen 4)
            synthesizeLinks(rows, 3, 4, effectiveLinks)
        }

        // Partner (horizontal) links
        effectiveLinks.filter { it.type == "partner" }.forEach { link ->
            val p1 = nodePos[link.from] ?: return@forEach
            val p2 = nodePos[link.to]   ?: return@forEach
            
            val x1 = if (p1.x < p2.x) p1.x + half else p1.x - half
            val x2 = if (p2.x < p1.x) p2.x + half else p2.x - half
            canvas.drawLine(x1, p1.y, x2, p2.y, linePaint)
        }

        // Parent → child drop lines
        val childToParents = mutableMapOf<String, MutableList<String>>()
        effectiveLinks.filter { it.type == "parent" }.forEach { link ->
            childToParents.getOrPut(link.to) { mutableListOf() }.add(link.from)
        }

        val familyGroups = mutableMapOf<Set<String>, MutableList<String>>()
        childToParents.forEach { (childId, parentIds) ->
            familyGroups.getOrPut(parentIds.toSet()) { mutableListOf() }.add(childId)
        }

        familyGroups.forEach { (parentIds, childIds) ->
            val parentPts = parentIds.mapNotNull { nodePos[it] }
            if (parentPts.isEmpty()) return@forEach

            // Drop origin
            val dropX = if (parentPts.size >= 2) (parentPts[0].x + parentPts[1].x) / 2f else parentPts[0].x
            val dropTopY = parentPts[0].y

            val childPts = childIds.mapNotNull { nodePos[it] }
            if (childPts.isEmpty()) return@forEach

            val childY = childPts[0].y
            val midY = (dropTopY + childY) / 2f

            sharedPath.reset()
            sharedPath.moveTo(dropX, dropTopY)
            sharedPath.lineTo(dropX, midY)
            // Horizontal bar above children
            val minX = childPts.minOfOrNull { it.x } ?: dropX
            val maxX = childPts.maxOfOrNull { it.x } ?: dropX
            val barL = minOf(minX, dropX)
            val barR = maxOf(maxX, dropX)
            sharedPath.moveTo(barL, midY)
            sharedPath.lineTo(barR, midY)
            
            childPts.forEach { cp ->
                sharedPath.moveTo(cp.x, midY)
                sharedPath.lineTo(cp.x, cp.y - half)
            }
            canvas.drawPath(sharedPath, linePaint)
        }
    }

    private fun synthesizeLinks(rows: Map<Int, List<PedigreeNode>>, parentGen: Int, childGen: Int, links: MutableList<PedigreeLink>) {
        val pNodes = rows[parentGen] ?: return
        val cNodes = rows[childGen] ?: return
        
        // Group parents by side
        val paternalParents = pNodes.filter { it.side_of_family == "Father Side" }
        val maternalParents = pNodes.filter { it.side_of_family == "Mother Side" }
        val centralParents  = pNodes.filter { it.side_of_family == "None" || it.side_of_family == null }

        // Logic: Paternal GP (Gen 0) -> Father (Gen 1)
        if (parentGen == 0 && childGen == 1) {
            val father = pNodes.find { it.relationship?.contains("Father", ignoreCase = true) == true && it.side_of_family != "Mother Side" } 
                         ?: cNodes.find { it.relationship?.contains("Father", ignoreCase = true) == true }
            val mother = pNodes.find { it.relationship?.contains("Mother", ignoreCase = true) == true && it.side_of_family != "Father Side" }
                         ?: cNodes.find { it.relationship?.contains("Mother", ignoreCase = true) == true }

            // Connect Paternal Grandparents (in Gen 0) to Father (in Gen 1)
            val pGPs = paternalParents.filter { it.gender?.lowercase() == "male" }
            val pGMs = paternalParents.filter { it.gender?.lowercase() == "female" }
            if (father != null) {
                if (pGPs.isNotEmpty() && pGMs.isNotEmpty()) {
                    links.add(PedigreeLink(from = pGPs[0].nodeId, to = pGMs[0].nodeId, type = "partner"))
                    links.add(PedigreeLink(from = pGPs[0].nodeId, to = father.nodeId, type = "parent"))
                    links.add(PedigreeLink(from = pGMs[0].nodeId, to = father.nodeId, type = "parent"))
                } else if (pGPs.isNotEmpty()) {
                    links.add(PedigreeLink(from = pGPs[0].nodeId, to = father.nodeId, type = "parent"))
                } else if (pGMs.isNotEmpty()) {
                    links.add(PedigreeLink(from = pGMs[0].nodeId, to = father.nodeId, type = "parent"))
                }
            }

            // Connect Maternal Grandparents (in Gen 0) to Mother (in Gen 1)
            val mGPs = maternalParents.filter { it.gender?.lowercase() == "male" }
            val mGMs = maternalParents.filter { it.gender?.lowercase() == "female" }
            if (mother != null) {
                if (mGPs.isNotEmpty() && mGMs.isNotEmpty()) {
                    links.add(PedigreeLink(from = mGPs[0].nodeId, to = mGMs[0].nodeId, type = "partner"))
                    links.add(PedigreeLink(from = mGPs[0].nodeId, to = mother.nodeId, type = "parent"))
                    links.add(PedigreeLink(from = mGMs[0].nodeId, to = mother.nodeId, type = "parent"))
                } else if (mGPs.isNotEmpty()) {
                    links.add(PedigreeLink(from = mGPs[0].nodeId, to = mother.nodeId, type = "parent"))
                } else if (mGMs.isNotEmpty()) {
                    links.add(PedigreeLink(from = mGMs[0].nodeId, to = mother.nodeId, type = "parent"))
                }
            }
        }

        // Logic: Father & Mother (Gen 1) -> Proband/Siblings (Gen 2)
        if (parentGen == 1 && childGen == 2) {
            val father = pNodes.find { it.relationship?.contains("Father", ignoreCase = true) == true }
            val mother = pNodes.find { it.relationship?.contains("Mother", ignoreCase = true) == true }
            
            if (father != null && mother != null) {
                links.add(PedigreeLink(from = father.nodeId, to = mother.nodeId, type = "partner"))
                cNodes.filter { it.side_of_family == "None" || it.side_of_family == null || it.isProband }.forEach { child ->
                    links.add(PedigreeLink(from = father.nodeId, to = child.nodeId, type = "parent"))
                    links.add(PedigreeLink(from = mother.nodeId, to = child.nodeId, type = "parent"))
                }
            }
        }
        
        // Fallback for simple cases if no links were added
        if (links.isEmpty() && pNodes.size >= 2 && cNodes.isNotEmpty()) {
            val f = pNodes.find { (it.gender ?: "male").lowercase() == "male" } ?: pNodes[0]
            val m = pNodes.find { (it.gender ?: "male").lowercase() == "female" } ?: pNodes[1]
            links.add(PedigreeLink(from = f.nodeId, to = m.nodeId, type = "partner"))
            cNodes.forEach { child ->
                links.add(PedigreeLink(from = f.nodeId, to = child.nodeId, type = "parent"))
                links.add(PedigreeLink(from = m.nodeId, to = child.nodeId, type = "parent"))
            }
        }
    }

    // ── Node drawing ─────────────────────────────────────────────────────────
    private fun drawNode(canvas: Canvas, node: PedigreeNode, cx: Float, cy: Float, nodeLabel: String) {
        val gender = (node.gender ?: "male").lowercase()
        val health = (node.healthStatus ?: "").lowercase()
        val half = nodeSize / 2f

        val nodeColor = when {
            health == "affected" -> colorAffected
            gender == "female"   -> colorFemale
            else                 -> colorMale
        }
        fillPaint.color = nodeColor

        if (gender == "female") {
            canvas.drawCircle(cx, cy, half, fillPaint)
            canvas.drawCircle(cx, cy, half, borderPaint)
        } else {
            sharedRect.set(cx - half, cy - half, cx + half, cy + half)
            canvas.drawRect(sharedRect, fillPaint)
            canvas.drawRect(sharedRect, borderPaint)
        }

        // Carrier dot
        if (health == "carrier") {
            canvas.drawCircle(cx, cy, half * 0.25f, carrierDotPaint)
        }

        // Labels
        var textY = cy + half + 45f
        
        // ID Label (e.g., I-1)
        canvas.drawText(nodeLabel, cx, textY, nodeIdPaint)
        textY += nodeIdPaint.textSize + 10f

        // Name (optional, useful for real patients)
        val nameToDraw = if (node.isProband) (node.fullName ?: "Patient") else (node.fullName ?: "")
        if (nameToDraw.isNotBlank()) {
            val p = if (node.isProband) probandNamePaint else namePaint
            val maxW = hSpacing - 40f
            
            val wrapped = wrappedTextCache.getOrPut("${node.nodeId}_$maxW") {
                wrapText(nameToDraw, p, maxW)
            }
            
            wrapped.forEach { line ->
                canvas.drawText(line, cx, textY, p)
                textY += p.textSize + 4f
            }
            textY += 6f
        }

        // Relationship label (e.g., Grandfather (Father Side))
        val side = node.side_of_family
        val relText = if (!side.isNullOrBlank() && side != "None") {
            "${node.relationship ?: ""} ($side)"
        } else {
            node.relationship ?: ""
        }

        if (relText.isNotBlank()) {
            val rp = if (node.isProband) probandBadgePaint else relPaint
            val maxW = hSpacing - 40f
            val wrappedRel = wrappedTextCache.getOrPut("${node.nodeId}_rel_$maxW") {
                wrapText(relText, rp, maxW)
            }
            wrappedRel.forEach { line ->
                canvas.drawText(line, cx, textY, rp)
                textY += rp.textSize + 4f
            }
            textY += 4f
        }

        // Status label (e.g., Affected)
        val statusText = if (health.isNotBlank()) health.replaceFirstChar { it.uppercase() } else "Unaffected"
        statusPaint.color = when (health) {
            "affected" -> colorAffected
            "carrier"  -> colorCarrier
            else       -> colorUnaffected
        }
        canvas.drawText(statusText, cx, textY, statusPaint)
        textY += statusPaint.textSize + 8f

        // Proband indicator arrow (pointing from left towards the node)
        if (node.isProband) {
            val arrowTailX = cx - half - 100f
            val arrowTipX  = cx - half - 15f
            val arrowY     = cy + (half * 0.4f) // slightly below center for visibility
            
            canvas.drawLine(arrowTailX, arrowY, arrowTipX, arrowY, arrowPaint)
            sharedPath.reset()
            sharedPath.moveTo(arrowTipX, arrowY)
            sharedPath.lineTo(arrowTipX - 22f, arrowY - 14f)
            sharedPath.lineTo(arrowTipX - 22f, arrowY + 14f)
            sharedPath.close()
            canvas.drawPath(sharedPath, arrowPaint)
        }
    }

    // ── Text wrapping helper ─────────────────────────────────────────────────
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        sharedStringBuilder.setLength(0)
        
        for (word in words) {
            val current = sharedStringBuilder.toString()
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) {
                if (current.isNotEmpty()) sharedStringBuilder.append(' ')
                sharedStringBuilder.append(word)
            } else {
                if (current.isNotEmpty()) lines.add(current)
                sharedStringBuilder.setLength(0)
                sharedStringBuilder.append(word)
            }
        }
        val last = sharedStringBuilder.toString()
        if (last.isNotEmpty()) lines.add(last)
        
        // cap at 2 lines
        return when {
            lines.size > 2 -> listOf(lines[0], lines[1] + "…")
            else           -> lines
        }
    }

    // ── Row builder ───────────────────────────────────────────────────────────
    private fun buildRows(nodes: List<PedigreeNode>): Map<Int, List<PedigreeNode>> {
        val rows = mutableMapOf<Int, MutableList<PedigreeNode>>()

        nodes.forEach { node ->
            val r = (node.relationship ?: "").lowercase()
            val f = (node.fullName ?: "").lowercase()
            
            // Check both relationship and full name for keywords
            val combined = "$r|$f"
            
            val genIndex = when {
                combined.contains("grandfather") || combined.contains("grandmother") || 
                combined.contains("grandpa") || combined.contains("grandma") ||
                combined.contains("gp") || combined.contains("grnd") -> 0
                
                (combined.contains("father") || combined.contains("mother") || 
                 combined.contains("papa") || combined.contains("mama") ||
                 combined.contains("dad") || combined.contains("mom") ||
                 combined.contains("uncle") || combined.contains("aunt") ||
                 combined.contains("maternal") || combined.contains("paternal")) && 
                 !combined.contains("grand") -> 1
                
                node.isProband || combined.contains("patient") || combined.contains("self") || 
                combined.contains("me") || combined.contains("proband") ||
                combined.contains("brother") || combined.contains("sister") || combined.contains("sibling") ||
                combined.contains("cousin") || combined.contains("husband") || combined.contains("wife") || 
                combined.contains("spouse") || combined.contains("partner") || combined.contains("mate") -> 2
                
                combined.contains("son") || combined.contains("daughter") || combined.contains("child") || 
                combined.contains("nephew") || combined.contains("niece") || combined.contains("kid") -> 3
                
                combined.contains("grandson") || combined.contains("granddaughter") || 
                combined.contains("grandchild") || combined.contains("gs") || combined.contains("gd") -> 4
                
                (f.contains("id") || f.contains("pt") || f.contains("patient")) && f.length < 15 -> 2 // Likely just a patient ID or label

                else -> 2 // Default to same generation as proband
            }
            if (!rows.containsKey(genIndex) || !rows[genIndex]!!.contains(node)) {
                rows.getOrPut(genIndex) { mutableListOf() }.add(node)
            }
        }

        // Sort Priority: Side (Father -> None -> Mother) -> Proband -> Gender -> Name
        return rows.filter { it.value.isNotEmpty() }.mapValues { entry ->
            entry.value.sortedWith(
                compareBy<PedigreeNode> {
                    when(it.side_of_family) {
                        "Father Side" -> 0
                        null, "None" -> 1
                        "Mother Side" -> 2
                        else -> 1
                    }
                }
                .thenByDescending { it.isProband }
                .thenByDescending { (it.gender ?: "male").lowercase() == "male" }
                .thenBy { it.fullName ?: "" }
            )
        }
    }

    private fun toRoman(n: Int) = when (n) {
        1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
        6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"; 10 -> "X"
        else -> n.toString()
    }
}