import SwiftUI
import GoogleMobileAds
import ComposeApp

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
        MobileAds.shared.start(completionHandler: nil)
        Task { @MainActor in
            AdMobManager.shared.setup()
        }
        return true
    }
}

// MARK: - AdMobManager

@MainActor
final class AdMobManager: NSObject, IosAdDelegate {
    static let shared = AdMobManager()

    private var rewardedAd: RewardedAd?
    private var pendingCallback: (any AdRewardCallback)?
    private let adUnitID = Bundle.main.object(forInfoDictionaryKey: "ADMOB_REWARDED_AD_UNIT_ID") as? String
        ?? "ca-app-pub-3940256099942544/1712485313" // fallback: тестовый iOS rewarded ID

    private override init() { super.init() }

    func setup() {
        IosAdBridge.shared.delegate = self
        loadAd()
    }

    // MARK: IosAdDelegate

    var isAdReady: Bool { rewardedAd != nil }

    func preload() {
        loadAd()
    }

    func showAd(callback: any AdRewardCallback) {
        guard let ad = rewardedAd,
              let root = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows.first?.rootViewController
        else {
            callback.onFailed()
            return
        }

        pendingCallback = callback
        ad.fullScreenContentDelegate = self
        ad.present(from: root, userDidEarnRewardHandler: { [weak self] in
            self?.pendingCallback?.onRewarded()
            self?.pendingCallback = nil
            self?.rewardedAd = nil
            self?.loadAd()
        })
    }

    // MARK: Private

    private func loadAd() {
        guard rewardedAd == nil else { return }
        RewardedAd.load(
            with: adUnitID,
            request: Request(),
            completionHandler: { [weak self] ad, error in
                if let error = error {
                    print("AdMob load failed: \(error.localizedDescription)")
                    return
                }
                self?.rewardedAd = ad
            }
        )
    }
}

extension AdMobManager: FullScreenContentDelegate {
    nonisolated func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("AdMob show failed: \(error.localizedDescription)")
        Task { @MainActor in
            self.pendingCallback?.onFailed()
            self.pendingCallback = nil
            self.rewardedAd = nil
            self.loadAd()
        }
    }
}
