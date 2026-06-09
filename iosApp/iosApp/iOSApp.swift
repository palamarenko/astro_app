import SwiftUI
import GoogleMobileAds

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Инициализируем Google Mobile Ads SDK
        GADMobileAds.sharedInstance().start(completionHandler: nil)

        // Подключаем AdMob к Kotlin-bridge
        Task { @MainActor in
            AdMobManager.shared.setup()
        }

        return true
    }
}
