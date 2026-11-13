document.addEventListener("DOMContentLoaded", () => {
    const catalog = window.tagCatalog;

    const tagTypeSelect = document.getElementById("tagType");
    const tagSelect = document.getElementById("tag");

    function updateTags() {
        const selectedType = tagTypeSelect.value;
        const tags = catalog[selectedType] || [];

        tagSelect.innerHTML = "";

        tags.forEach(tag => {
            const option = document.createElement("option");
            option.value = tag;
            option.textContent = tag;
            tagSelect.appendChild(option);
        });
    }

    tagTypeSelect.addEventListener("change", updateTags);

    updateTags();
});