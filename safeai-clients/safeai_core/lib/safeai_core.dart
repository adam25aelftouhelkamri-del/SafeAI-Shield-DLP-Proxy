import 'package:dio/dio.dart';
import 'package:safeai_core/src/token_storage.dart';

export 'src/token_storage.dart';
import 'src/auth_interceptor.dart';

class AuthService {
  // 1. create the Dio object
  final Dio dio = Dio();

  // 3. a TokenStorage field
  final TokenStorage tokenStorage = TokenStorage();

  final AuthInterceptor authInterceptor= AuthInterceptor();

  AuthService(){
    dio.interceptors.add(authInterceptor);
  }
  // 2. the login method
  Future<void> login(String username, String password) async {
    try {
      // 3. POST to https://dummyjson.com/auth/login
      //    with a map: username, password, expiresInMins: 1
      final response = await dio.post(
        'https://dummyjson.com/auth/login',
        data: {
          'username': username,
          'password': password,
          'expiresInMins': 30,
        }, // the body, as a map
      );
      // 4. print the status code
      print('Status: ${response.statusCode}');
      // 5. print the first 20 characters of accessToken and refreshToken
      String resultAccess = response.data['accessToken'].substring(0, 20);
      String resultRefresh = response.data['refreshToken'].substring(0, 20);
      print('Access Token (20C): ${resultAccess}');
      print('Refresh Token (20C): ${resultRefresh}');
      // 4. save the FULL tokens (see the trap below)
      await tokenStorage.saveTokens(
        response.data['accessToken'],
        response.data['refreshToken'],
      );
    } on DioException catch (e) {
      print('Error: ${e.response?.statusCode}'); // failure: 400, 401...
    }
  }

  Future<void> getMe() async {
    //    inside try / on DioException catch, like login
    try {
      // 3. NEW: getMe() → GET https://dummyjson.com/auth/me
      final response = await dio.get(
        'https://dummyjson.com/auth/me',
      );
      //    print the status code and response.data['firstName']

      print('Name: ${response.data['firstName']}');
      print('Status: ${response.statusCode}');

    } on DioException catch (e) {
      print('Error: ${e.response?.statusCode}');
    }
  }
}
