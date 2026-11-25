import 'package:safeai_core/safeai_core.dart';
import 'package:material_ui/material_ui.dart';

void main() async {
  // 1. make Flutter ready before using a plugin
  WidgetsFlutterBinding.ensureInitialized();
  // 2. create a TokenStorage
  TokenStorage tokenStorage=TokenStorage();

  // 3. read the stored access token (with await)
  String? accessToken = await tokenStorage.getAccessToken();
  String?  refreshToken = await tokenStorage.getRefreshToken();

  // 4. if it's not null → print 'Stored access token: ' + its first 20 characters
  if (accessToken!=null && refreshToken!=null) {
    print('Stored access token: ${accessToken.substring(0, 20)}');
    print('Stored refresh token: ${refreshToken.substring(0, 20)}');
  } else {
    //    otherwise → print 'No token stored'
    print("No token stored");
  }

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(

        colorScheme: .fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});



  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;

  void _incrementCounter() async {
    setState(() {
      _counter++;
    });
    final auth = AuthService();
    await auth.login('emilys', 'emilyspass');
    await auth.getMe();
  }


  @override
  Widget build(BuildContext context) {

    return Scaffold(
      appBar: AppBar(

        backgroundColor: Theme.of(context).colorScheme.inversePrimary,

        title: Text(widget.title),
      ),
      body: Center(

        child: Column(

          mainAxisAlignment: .center,
          children: [
            const Text('You have pushed the button this many times:'),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
    );
  }
}
