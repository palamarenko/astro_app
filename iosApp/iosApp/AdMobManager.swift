import GoogleMobileAds
import ComposeApp

@MainActor
final class AdMobManager: NSObject {
    static let shared = AdMobManager()

    private var rewardedAd: GADRewardedAd?
    private let adUnitID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // замени на свой ID

    private override init() { super.init() }

    func setup() {
        // Передаём callbacks в Kotlin-bridge
        IosAdBridge.shared.preload = { [weak self] in
            self?.loadAd()
        }
        IosAdBridge.shared.showAd = { [weak self] onRewarded, onFailed in
            self?.showAd(onRewarded: onRewarded, onFailed: onFailed)
        }
        loadAd()
    }

    private func loadAd() {
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: adUnitID, request: request) { [weak self] ad, error in
            if let error = error {
                print("AdMob load failed: \(error.localizedDescription)")
                IosAdBridge.shared.adReady = false
                return
            }
            self?.rewardedAd = ad
            IosAdBridge.shared.adReady = true
        }
    }

    private func showAd(onRewarded: @escaping () -> Void, onFailed: @escaping () -> Void) {
        guard let ad = rewardedAd,
              let root = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows.first?.rootViewController
        else {
            onFailed()
            return
        }

        ad.fullScreenContentDelegate = self
        ad.present(fromRootViewController: root) {
            onRewarded()
            // Перезагружаем следующую рекламу
            IosAdBridge.shared.adReady = false
            self.rewardedAd = nil
            self.loadAd()
        }
    }
}

extension AdMobManager: GADFullScreenContentDelegate {
    nonisolated func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("AdMob show failed: \(error.localizedDescription)")
        Task { @MainActor in
            IosAdBridge.shared.adReady = false
            self.rewardedAd = nil
            self.loadAd()
        }
    }
}
