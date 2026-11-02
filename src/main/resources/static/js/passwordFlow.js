const form = document.getElementById("form");
const loading = document.getElementById("loading");

form.style.display = "none";
loading.style.display = "flex";

const urlParams = new URLSearchParams(window.location.search);
const secretKey = urlParams.get("key");

if (secretKey == null || secretKey.length <= 0) {
    //window.location.href = "/auth/login";
}

const keyInput = document.getElementById("secretKey");
keyInput.value = secretKey;

const newPass = document.getElementById("newPassword");
const confirmPass = document.getElementById("confirmPassword");
const submitButton = document.getElementById("submit");

submitButton.disabled = true;
confirmPass.addEventListener("input", () => {
    submitButton.disabled = newPass.value != confirmPass.value;
})

loading.style.display = "none";
form.style.display = "flex";
