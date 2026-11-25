import 'package:dio/dio.dart';
import 'token_storage.dart';

class AuthInterceptor extends Interceptor {
  // 1. a TokenStorage field, to read the token
  TokenStorage storage=TokenStorage();
  // 2. this runs automatically BEFORE every request leaves
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    // 3. if this is the login request → pass it on unchanged, and stop here
    if(options.path.contains("/auth/login")){
      handler.next(options);
      return;
    }

    // 4. read the access token from storage
    String? accessToken = await storage.getAccessToken();
    String method=options.method;
    String path=options.path;

    // 5. if there is one → add the header  Authorization: Bearer <token>
    if(accessToken!=null){
      options.headers['Authorization'] = 'Bearer $accessToken';
      // 6. print which request got the badge (for testing)
      print("Badge added to: $method  $path");
    }
    // 7. ALWAYS pass the request on, or it never leaves
    handler.next(options);

  }
}