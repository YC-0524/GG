const form = document.getElementById("form");
const loading = document.getElementById("loading");

form.style.display = "none";
loading.style.display = "flex";

const urlParams = new URLSearchParams(window.location.search);
const secretKey = urlParams.get("key");

if (secretKey == null || secretKey.length <= 0) {
    //window.location.href = "/auth/login";
}

document.getElementById("secretKey").value = secretKey;

const error = urlParams.get("error");

if (error) {
    const errorMessage = document.getElementById("error-message");

    errorMessage.textContent = error;
    errorMessage.hidden = false;
}

loading.style.display = "none";
form.style.display = "flex";
