package com.simats.genetics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.simats.genetics.network.responses.PedigreeData
import com.simats.genetics.network.responses.PedigreeLink
import com.simats.genetics.network.responses.PedigreeNode
import kotlin.math.max

class PedigreeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pedigreeData: PedigreeData? = null

    // ---- paints ----
    private val linePaint = Paint().apply {
        color = Color.parseColor("#1A1F26")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#1A1F26")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val shadowPaint = Paint().apply {
        color = Color.TRANSPARENT
        setShadowLayer(10f, 0f, 4f, Color.parseColor("#40000000"))
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#1A1F26")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val genLabelPaint = Paint().apply {
        color = Color.parseColor("#757B8C")
        textSize = 36f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val subTextPaint = Paint().apply {
        color = Color.parseColor("#757B8C")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val statusPaint = Paint().apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val carrierDotPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val probandTextPaint = Paint(textPaint).apply {
        color = Color.parseColor("#1C57D9")
    }

    private val probandDecorPaint = Paint().apply {
        color = Color.parseColor("#1C57D9")
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 4f
        isAntiAlias = true
        textSize = 50f
    }

    // ---- layout constants ----
    private val nodeSize = 130f
    private val horizontalSpacing = 260f
    private val verticalSpacing = 480f
    private val topPadding = 200f
    private val sidePadding = 250f

    private val nodePos = mutableMapOf<String, PointF>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for setShadowLayer on some versions
    }

    fun setData(data: PedigreeData) {
        this.pedigreeData = data
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = pedigreeData
        if (d == null || d.nodes.isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val rows = buildRows(d.nodes)
        val maxCols = if (rows.isNotEmpty()) rows.map { it.size }.maxOrNull() ?: 1 else 1
        val totalRows = max(1, rows.size)

        val desiredW = (sidePadding * 2 + (maxCols - 1) * horizontalSpacing + nodeSize).toInt()
        val desiredH = (topPadding + (totalRows - 1) * verticalSpacing + nodeSize + 400).toInt()

        setMeasuredDimension(
            resolveSize(desiredW, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = pedigreeData ?: return
        if (d.nodes.isNullOrEmpty()) return

        nodePos.clear()
        val rows = buildRows(d.nodes)

        // 1. Calculate positions and draw Generation Labels
        rows.forEachIndexed { rowIndex, rowNodes ->
            val y = topPadding + rowIndex * verticalSpacing
            val rowWidth = (rowNodes.size - 1) * horizontalSpacing
            val startX = width / 2f - rowWidth / 2f

            // Generation Label
            canvas.drawText("Generation ${toRoman(rowIndex + 1)}", 40f, y - 120f, genLabelPaint)

            rowNodes.forEachIndexed { colIndex, node ->
                val x = startX + colIndex * horizontalSpacing
                nodePos[node.nodeId] = PointF(x, y)
            }
        }

        // 2. Draw Links
        drawLinks(canvas, d)

        // 3. Draw Nodes
        d.nodes.forEach { node ->
            val p = nodePos[node.nodeId] ?: return@forEach
            drawNode(canvas, node, p.x, p.y)
        }
    }

    private fun drawLinks(canvas: Canvas, d: PedigreeData) {
        val partnerLinks = d.links?.filter { it.type == "partner" } ?: emptyList()
        partnerLinks.forEach { link ->
            val p1 = nodePos[link.from]
            val p2 = nodePos[link.to]
            if (p1 != null && p2 != null) {
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, linePaint)
            }
        }

        // Parent-child relationships
        val childToParents = mutableMapOf<String, MutableList<String>>()
        val parentLinks = d.links?.filter { it.type == "parent" } ?: emptyList()
        parentLinks.forEach { link ->
            childToParents.getOrPut(link.to) { mutableListOf() }.add(link.from)
        }

        val partnershipsWithChildren = mutableMapOf<Set<String>, MutableList<String>>()
        childToParents.forEach { (childId, parentIds) ->
            partnershipsWithChildren.getOrPut(parentIds.toSet()) { mutableListOf() }.add(childId)
        }

        partnershipsWithChildren.forEach { (parentSet, childrenIds) ->
            val parentPoints = parentSet.mapNotNull { nodePos[it] }
            if (parentPoints.isEmpty()) return@forEach

            val dropOriginX = if (parentPoints.size >= 2) {
                (parentPoints[0].x + parentPoints[1].x) / 2f
            } else {
                parentPoints[0].x
            }
            val dropOriginY = parentPoints[0].y

            val dropHeight = 150f
            val siblingBarY = dropOriginY + dropHeight
            canvas.drawLine(dropOriginX, dropOriginY, dropOriginX, siblingBarY, linePaint)

            val childPoints = childrenIds.mapNotNull { nodePos[it] }
            if (childPoints.isNotEmpty()) {
                val minX = childPoints.minOf { it.x }
                val maxX = childPoints.maxOf { it.x }
                canvas.drawLine(minX, siblingBarY, maxX, siblingBarY, linePaint)

                childPoints.forEach { cp ->
                    canvas.drawLine(cp.x, siblingBarY, cp.x, cp.y, linePaint)
                }
            }
        }
    }

    private fun drawNode(canvas: Canvas, node: PedigreeNode, x: Float, y: Float) {
        val gender = (node.gender ?: "").lowercase()
        val health = (node.healthStatus ?: "").lowercase()

        val pinkColor = Color.parseColor("#FF66B2")
        val blueColor = Color.parseColor("#6391F2")
        val redColor = Color.parseColor("#FF1744")
        val orangeColor = Color.parseColor("#FF9800")
        val greenColor = Color.parseColor("#28A745")

        val baseColor = if (gender == "female") pinkColor else blueColor
        val fillColor = when (health) {
            "affected" -> redColor
            else -> baseColor
        }

        val fillPaint = Paint().apply {
            color = fillColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Drop shadow
        if (gender == "female") {
            canvas.drawCircle(x, y, nodeSize / 2f, shadowPaint)
            canvas.drawCircle(x, y, nodeSize / 2f, fillPaint)
            canvas.drawCircle(x, y, nodeSize / 2f, borderPaint)
            if (health == "carrier") {
                canvas.drawCircle(x, y, 16f, carrierDotPaint)
            }
        } else {
            val rect = RectF(x - nodeSize / 2f, y - nodeSize / 2f, x + nodeSize / 2f, y + nodeSize / 2f)
            canvas.drawRect(rect, shadowPaint)
            canvas.drawRect(rect, fillPaint)
            canvas.drawRect(rect, borderPaint)
        }

        // Texts
        val nameToDisplay = if (node.isProband) "You" else (node.fullName ?: "")
        val activeTextPaint = if (node.isProband) probandTextPaint else textPaint

        canvas.drawText(nameToDisplay, x, y + nodeSize + 40f, activeTextPaint)
        canvas.drawText(node.relationship ?: "", x, y + nodeSize + 80f, subTextPaint)

        statusPaint.color = when (health) {
            "affected" -> redColor
            "unaffected" -> greenColor
            "carrier" -> orangeColor
            else -> Color.parseColor("#757B8C")
        }
        canvas.drawText(node.healthStatus ?: "", x, y + nodeSize + 120f, statusPaint)

        // Proband Decoration: "[■ →] You"
        if (node.isProband) {
            val decorX = x - (nodeSize / 2f) - 80f
            // Square Bullet
            canvas.drawRect(decorX - 15f, y - 5f, decorX + 15f, y + 15f, probandDecorPaint)
            // Arrow
            canvas.drawText("→", decorX + 40f, y + 15f, probandDecorPaint)
        }
    }

    private fun buildRows(nodes: List<PedigreeNode>): List<List<PedigreeNode>> {
        val gen1 = mutableListOf<PedigreeNode>()
        val gen2 = mutableListOf<PedigreeNode>()
        val others = mutableListOf<PedigreeNode>()

        nodes.forEach { n ->
            val rel = (n.relationship ?: "").lowercase()
            when {
                rel.contains("father") || rel.contains("mother") || rel.contains("grandfather") || rel.contains("grandmother") -> gen1.add(n)
                n.isProband || rel.contains("brother") || rel.contains("sister") || rel.contains("son") || rel.contains("daughter") -> gen2.add(n)
                else -> others.add(n)
            }
        }

        val rows = mutableListOf<List<PedigreeNode>>()
        if (gen1.isNotEmpty()) {
            // Sort to have Male on left if possible
            rows.add(gen1.sortedByDescending { (it.gender ?: "").lowercase() == "male" })
        }
        if (gen2.isNotEmpty()) {
            rows.add(gen2)
        }
        if (others.isNotEmpty()) {
            others.chunked(4).forEach { rows.add(it) }
        }
        return if (rows.isEmpty()) listOf(nodes) else rows
    }

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> number.toString()
        }
    }
}