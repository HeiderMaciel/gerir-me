var loginModule  = angular.module('LoginModule',['br']);
var LoginController = function($scope, $http){
	var loginSoccess = function(r){
		eval('var result ='+r);
		if(result.success){
			if(result.companys.length ==1){
				document.location.href = result.goTo;
			}else{
				$scope.companys = result.companys;
			}
		}else{
			alert(result.message);
		}
	};
	var loginError = function(r){
		alert("Erro ao efetuar login, tente novamente!");
	};
	//
	//  Rigel - 03/03/2017
	//  ALTERACAO DE VERSAO DO APLICATIVO DEVE SER FEITA no face_prepare.js
	//
	$scope.loginWithFacebook = function(){
		var face = new EbelleFacebook();
		face.getUserByFacebook().then(function(userData){
			if(userData){//&& userData.length === 1
				var user = userData[0];
				$http.post("/security/login_face", {company : user.company, id: user.id, facebookId : user.facebookId, facebookAccessToken : user.facebookAccessToken}).success(loginSoccess).error(loginError);
			}
		}).then(null,function(){
			alert('Erro ao fazer login com facebook, tente novamente!');
		});
	};
	$scope.loginWithCompany = function(company, email, password){
		$http.post("/security/login_email", {company : company,email : email, password : password, hasCompany : true}).success(loginSoccess).error(loginError);
	};
	$scope.login = function(email, password){
		if (email == undefined || email == "") {
			alert ("Por favor informe um email!");
			return
		}
		if (password == undefined || password == "") {
			alert ("Por favor informe uma senha!")
			return
		}
		if (document.location.href.indexOf("local") != -1) {
			$http.post("/security/login_email", {company : "", email : email, password : password, hasCompany : false}).success(loginSoccess).error(loginError);
		} else {
			$http.post("/security/login_email", {company : "", email : email, password : password, hasCompany : false}).success(loginSoccess).error(loginError);
			//$http.post("http://45.33.99.152:7171/security/login_email", {company : "", email : email, password : password, hasCompany : false}).success(loginSoccess).error(loginError);
		}
	};
	$scope.rememberPassword = function(email){
		var emailValidation = /^([a-z0-9._%\-+]+@(?:[a-z0-9\-]+\.)+[a-z]{2,4}$)/;
		if (!emailValidation.test(email) && email.length > 10){
			alert ('E-mail inválido!\n')
			return;
		} else if (email.indexOf("@") <= 1) {
			alert ('E-mail inválido!\n')
			return;
		} else {
			//alert ('E-mail ok!\n')
		}
		$http.post("/security/remember_user_password", {email : email}).success(function(results){
	        if(results === 1 || results == "1"){
				alert("Enviado com sucesso para " + email);
	        }else{
	          alert(eval(results));
	        }
		}).error(function(){
			alert("Erro ao processar requisição!");
		});	
	}
};
