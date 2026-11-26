// Profile pictures: show the initial if an image fails to load,
// and let the owner click their picture to upload a new one
const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
const ALLOWED_AVATAR_TYPES = ["image/jpeg", "image/png", "image/webp"];

document.querySelectorAll(".profile-picture img").forEach((image) => {
    image.addEventListener("error", () => image.remove());
});

const avatarButton = document.getElementById("avatar-edit-button");
const avatarPanel = document.getElementById("avatar-panel");

if (avatarButton && avatarPanel) {
    const input = document.getElementById("avatar-input");
    const preview = document.getElementById("avatar-preview-image");
    const clientError = document.getElementById("avatar-client-error");
    const cancelButton = document.getElementById("avatar-cancel");
    let previewUrl = null;

    function openPanel() {
        avatarPanel.hidden = false;
        avatarButton.setAttribute("aria-expanded", "true");
        input.focus();
    }

    function clearPreview() {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }
        preview.hidden = true;
        preview.removeAttribute("src");
    }

    function closePanel() {
        input.value = "";
        clientError.textContent = "";
        input.setCustomValidity("");
        clearPreview();
        avatarPanel.hidden = true;
        avatarButton.setAttribute("aria-expanded", "false");
        avatarButton.focus();
    }

    // quick checks in the browser; the server checks everything again
    function problemWith(file) {
        if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
            return "Only JPG, PNG or WebP images are allowed.";
        }
        if (file.size > MAX_AVATAR_BYTES) {
            return "Your image must be 2 MB or smaller.";
        }
        return "";
    }

    input.addEventListener("change", () => {
        clearPreview();
        const file = input.files[0];
        if (!file) {
            clientError.textContent = "";
            input.setCustomValidity("");
            return;
        }

        const problem = problemWith(file);
        clientError.textContent = problem;
        input.setCustomValidity(problem);
        if (problem) {
            return;
        }

        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
        preview.hidden = false;
    });

    avatarButton.addEventListener("click", () => {
        if (avatarPanel.hidden) {
            openPanel();
        } else {
            closePanel();
        }
    });
    cancelButton.addEventListener("click", closePanel);
    avatarPanel.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closePanel();
        }
    });

    // after a failed upload, reopen the panel so the player can try again
    if (document.querySelector("[data-avatar-error]")) {
        avatarPanel.hidden = false;
        avatarButton.setAttribute("aria-expanded", "true");
    }
}
