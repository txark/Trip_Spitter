function showCustomAlert(message) {

    const overlay = document.createElement('div');

    overlay.className = 'custom-alert-overlay';



    const box = document.createElement('div');

    box.className = 'custom-alert-box';



    const p = document.createElement('p');

    p.innerText = message;



    const btn = document.createElement('button');

    btn.className = 'custom-alert-btn';

    btn.innerText = 'OK';



    btn.onclick = function() {

        overlay.remove();

    };



    box.appendChild(p);

    box.appendChild(btn);

    overlay.appendChild(box);

    document.body.appendChild(overlay);



    btn.focus();

}

