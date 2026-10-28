async function loadDashboard() {
    const welcome = document.getElementById("welcome");
    const moderator = document.getElementById("moderator");
    const admin = document.getElementById("admin");

    // temporary test user
    const user = {
    username: "TestUser",
    role: new URLSearchParams(window.location.search).get("role") || "USER"
    };

    try {

        welcome.textContent = `Welcome ${user.username}! You are a ${user.role}.`;

        if (user.role === "MODERATOR" || user.role === "ADMIN") {
            moderator.hidden = false;
        }

        if (user.role === "ADMIN") {
            admin.hidden = false;
        }

    } catch (error) {
        welcome.textContent = "Error. Please try again later.";
    }
}


document.addEventListener("DOMContentLoaded", loadDashboard);