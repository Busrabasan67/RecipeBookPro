Firebase Hosting ile tiklanabilir paylasim (varsayilan)
======================================================

Uygulama paylasiminda https://recipebookpro-ee941.web.app/open.html adresi kullanilir.
Bu sayfa ilk kez "firebase deploy" ile Internete konmussa WhatsApp ustte MAVI tiklanabilir link gosterir.

Tek sefer:
  1) npm i -g firebase-tools
  2) Proje kokunde: firebase login
  3) firebase deploy --only hosting

(open.html repo icinde sharing/open.html olarak durur; firebase.json "sharing" klasorunu yayinlar.)

Ozellestirme:
  local.properties icinde SHARE_HTTPS_REDIRECT_BASE=... ile baska bir open.html adresi verebilirsin.
