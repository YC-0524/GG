document.addEventListener("DOMContentLoaded", () => {
    const gameInput = document.getElementById("game-search");
    const rankSelect = document.getElementById("game-rank");
    const addButton = document.getElementById("add-game-button");
    const searchStatus = document.getElementById("game-search-status");
    const games = Array.from(document.querySelectorAll("#game-options option"));

    let selectedGame = null;

    function fillRanks(game) {
        rankSelect.replaceChildren(new Option("Unranked", "Unranked"));
        const ranks = game && game.dataset.ranks ? game.dataset.ranks.split("|") : [];
        ranks.forEach(rank => rankSelect.add(new Option(rank, rank)));
    }

    // Add game only unlocks once the text exactly matches a game in the list.
    function updateSearch() {
        const query = gameInput.value.trim().toLowerCase();
        const match = games.find(game => game.value.toLowerCase() === query) || null;
        const anyMatch = games.some(game => game.value.toLowerCase().includes(query));

        if (match !== selectedGame) {
            selectedGame = match;
            fillRanks(match);
        }

        rankSelect.disabled = !match;
        addButton.disabled = !match;
        searchStatus.textContent = query && !anyMatch ? "No games found" : "";
    }

    gameInput.addEventListener("input", updateSearch);
    updateSearch();

    // Remove needs a second click to confirm; some browsers block confirm() pop-ups.
    document.querySelectorAll(".remove-game-form").forEach(form => {
        const button = form.querySelector("button");
        form.addEventListener("submit", event => {
            if (form.dataset.confirming === "true") return;
            event.preventDefault();
            form.dataset.confirming = "true";
            button.textContent = "Confirm remove";
            button.setAttribute("aria-label", `Confirm removing ${form.dataset.game}`);
            setTimeout(() => {
                form.dataset.confirming = "";
                button.textContent = "Remove";
                button.removeAttribute("aria-label");
            }, 4000);
        });
    });
});
