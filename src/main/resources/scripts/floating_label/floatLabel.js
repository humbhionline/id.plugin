(function () {
    var inputs = document.querySelectorAll('input');
    for (var key in inputs) {
        // input
        var el = inputs[key];
        // el.getAttribute('data-class') && el.classList.add(el.getAttribute('data-class'));
        console.log("inner loop=", el);
        // label
        let label = document.createElement('label');
        label.innerHTML = el.getAttribute("placeholder");
        label.htmlFor = el.getAttribute("id")

        // wrapper Element
        var wrapper = document.createElement('div');
        wrapper.classList.add("wrapper");

        el.parentNode.insertBefore(wrapper, el);
        // move el into wrapper
        wrapper.appendChild(el);
        wrapper.appendChild(label);
    }

})();