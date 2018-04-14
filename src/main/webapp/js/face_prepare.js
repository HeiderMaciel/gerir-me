// quando a versao da aplicação no face era 2.5 aqui tava 2.3
// agora a versao no face ta v2.6 e aqui teve que ser 2.4
// 13/04/2018
window.fbAsyncInit = function() {
FB.init({
  appId      : '432866086802252',
  xfbml      : true,
  version    : 'v2.4'
});
};

(function(d, s, id){
 var js, fjs = d.getElementsByTagName(s)[0];
 if (d.getElementById(id)) {return;}
 js = d.createElement(s); js.id = id;
 js.src = "//connect.facebook.net/en_US/sdk.js";
 fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));