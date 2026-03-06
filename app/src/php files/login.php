<?php
header("Content-Type: application/json");
include "db.php";

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

// Validate JSON
if (!$data) {
    echo json_encode([
        "status" => false,
        "message" => "Invalid JSON input"
    ]);
    exit;
}

// Get inputs
$email = trim($data["email"] ?? "");
$password = $data["password"] ?? "";

// Check empty fields
if (empty($email) || empty($password)) {
    echo json_encode([
        "status" => false,
        "message" => "Email and Password are required"
    ]);
    exit;
}

// Check user exists
$sql = "SELECT id, full_name, email, phone, password FROM users WHERE email = ?";
$stmt = mysqli_prepare($conn, $sql);
mysqli_stmt_bind_param($stmt, "s", $email);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

if (mysqli_num_rows($result) == 0) {
    echo json_encode([
        "status" => false,
        "message" => "Invalid email or password"
    ]);
    exit;
}

$user = mysqli_fetch_assoc($result);

// Verify password
if (!password_verify($password, $user["password"])) {
    echo json_encode([
        "status" => false,
        "message" => "Invalid email or password"
    ]);
    exit;
}

// Login success
echo json_encode([
    "status" => true,
    "message" => "Login successful",
    "data" => [
        "id" => $user["id"],
        "full_name" => $user["full_name"],
        "email" => $user["email"],
        "phone" => $user["phone"]
    ]
]);
?>