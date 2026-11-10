document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("game-search");
    const input = document.getElementById("game-search-input");
    const panel = document.getElementById("game-search-panel");
    const empty = document.getElementById("game-search-empty");
    const groups = Array.from(panel.querySelectorAll(".game-group"));

    const visibleOptions = () =>
        Array.from(panel.querySelectorAll(".game-option")).filter(option => !option.parentElement.hidden);

    function openPanel() {
        panel.hidden = false;
        input.setAttribute("aria-expanded", "true");
    }

    function closePanel() {
        panel.hidden = true;
        input.setAttribute("aria-expanded", "false");
    }

    // Typing narrows each list; a list with no matches is hidden.
    function filter() {
        const query = input.value.trim().toLowerCase();
        let anyMatch = false;

        groups.forEach(group => {
            let groupMatch = false;
            group.querySelectorAll("li").forEach(item => {
                const name = item.querySelector(".game-option").dataset.name.toLowerCase();
                const match = name.includes(query);
                item.hidden = !match;
                groupMatch = groupMatch || match;
            });
            group.hidden = !groupMatch;
            anyMatch = anyMatch || groupMatch;
        });

        empty.hidden = anyMatch;
    }

    input.addEventListener("focus", openPanel);
    input.addEventListener("click", openPanel);
    input.addEventListener("input", () => {
        filter();
        openPanel();
    });

    // Enter opens the first game still in the list.
    form.addEventListener("submit", event => {
        const first = visibleOptions()[0];
        event.preventDefault();
        if (first) window.location.href = first.href;
    });

    // Arrow keys move through the list; Escape closes it.
    form.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closePanel();
            input.focus();
            return;
        }
        if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;

        const options = visibleOptions();
        if (options.length === 0) return;
        event.preventDefault();
        openPanel();

        const index = options.indexOf(document.activeElement);
        if (event.key === "ArrowDown") {
            options[index < options.length - 1 ? index + 1 : 0].focus();
        } else if (index <= 0) {
            input.focus();
        } else {
            options[index - 1].focus();
        }
    });

    // Clicking anywhere else closes the list.
    document.addEventListener("click", event => {
        if (!form.contains(event.target)) closePanel();
    });

    filter();
    if (document.activeElement === input) openPanel();
});