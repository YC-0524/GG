function displayReport(button) {
    button.closest(".report-wrapper")
        .querySelector(".report-popup").style.display = "grid";
}

function hideReport(button) {
    button.closest(".report-popup").style.display = "none";
}