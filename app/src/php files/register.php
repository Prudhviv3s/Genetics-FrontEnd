<?php
header("Content-Type: application/json");
include "db.php";

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

// Check if data received
if (!$data) {
    echo json_encode([
        "status" => false,
        "message" => "Invalid JSON input"
    ]);
    exit;
}

// Get fields
$full_name = trim($data["full_name"] ?? "");
$email = trim($data["email"] ?? "");
$phone = trim($data["phone"] ?? "");
$password = $data["password"] ?? "";
$confirm_password = $data["confirm_password"] ?? "";

// Validate empty fields
if (empty($full_name) || empty($email) || empty($phone) || empty($password) || empty($confirm_password)) {
    echo json_encode([
        "status" => false,
        "message" => "All fields are required"
    ]);
    exit;
}

// Check password match
if ($password !== $confirm_password) {
    echo json_encode([
        "status" => false,
        "message" => "Password and Confirm Password do not match"
    ]);
    exit;
}

// Check email already exists
$check = "SELECT id FROM users WHERE email = ?";
$stmt = mysqli_prepare($conn, $check);
mysqli_stmt_bind_param($stmt, "s", $email);
mysqli_stmt_execute($stmt);
mysqli_stmt_store_result($stmt);

if (mysqli_stmt_num_rows($stmt) > 0) {
    echo json_encode([
        "status" => false,
        "message" => "Email already registered"
    ]);
    exit;
}

// Hash password
$hashed_password = password_hash($password, PASSWORD_DEFAULT);

// Insert user
$sql = "INSERT INTO users (full_name, email, phone, password) VALUES (?, ?, ?, ?)";
$stmt = mysqli_prepare($conn, $sql);
mysqli_stmt_bind_param($stmt, "ssss", $full_name, $email, $phone, $hashed_password);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode([
        "status" => true,
        "message" => "Registration successful"
    ]);
} else {
    echo json_encode([
        "status" => false,
        "message" => "Registration failed"
    ]);
}
?>