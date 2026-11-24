import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  // 1. the safe object (FlutterSecureStorage)
  final FlutterSecureStorage storage = FlutterSecureStorage();

  // 2. the two key names, as constants (the "locker labels")
  static const String accessTokenKey="access_token";
  static const String refreshTokenKey="refresh_token";

  // 3. save both tokens
  Future<void> saveTokens(String accessToken, String refreshToken) async {
    // write the access token under its key
    await storage.write(key: accessTokenKey, value: accessToken);
    // write the refresh token under its key
    await storage.write(key: refreshTokenKey, value: refreshToken);
  }

  // 4. read the access token (can be null if nothing is saved)
  Future<String?> getAccessToken() async {
    // read it and return it
    return storage.read(key: accessTokenKey);
  }

  // 5. read the refresh token
  Future<String?> getRefreshToken() async {
    // read it and return it
    return storage.read(key: refreshTokenKey);

  }

  // 6. delete both (for logout, step 6)
  Future<void> clear() async {
    // delete the access token
    await storage.delete(key: accessTokenKey);
    // delete the refresh token
    await storage.delete(key: refreshTokenKey);

  }
}