// lets the profile owner click their name to edit it in place
const editButton = document.getElementById("username-edit-button");
const usernameForm = document.getElementById("username-form");

if (editButton && usernameForm) {
    const display = editButton.closest(".username-display");
    const input = document.getElementById("username-input");
    const cancelButton = document.getElementById("username-cancel");
    const originalName = input.value;

    function openEditor() {
        display.hidden = true;
        usernameForm.hidden = false;
        input.focus();
        input.select();
    }

    function closeEditor() {
        input.value = originalName;
        usernameForm.hidden = true;
        display.hidden = false;
        editButton.focus();
    }

    editButton.addEventListener("click", openEditor);
    cancelButton.addEventListener("click", closeEditor);
    usernameForm.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeEditor();
        }
    });
}
