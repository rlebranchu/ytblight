import XCTest

/// Tests d'interface sur l'application hôte « fakes » : aucun appel à Google, aucun compte réel.
final class YtblightUITests: XCTestCase {
    private func launch(signedIn: Bool) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-signedIn", signedIn ? "YES" : "NO"]
        app.launch()
        return app
    }

    func testSignedInUserSeesTheFeedWithoutShorts() {
        let app = launch(signedIn: true)

        XCTAssertTrue(app.staticTexts["Un long format sur Kotlin"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["24:10"].exists)
        XCTAssertTrue(app.staticTexts["Chaîne Kotlin · il y a 5 jours"].exists)
        XCTAssertFalse(app.staticTexts["Un court extrait"].exists)
    }

    func testFeedIsSortedFromNewestToOldest() {
        let app = launch(signedIn: true)
        XCTAssertTrue(app.staticTexts["Un long format sur Kotlin"].waitForExistence(timeout: 10))

        let titles = ["Conférence d'une heure", "Visite guidée d'un atelier", "Un long format sur Kotlin"]
        let tops = titles.map { app.staticTexts[$0].frame.minY }
        XCTAssertEqual(tops, tops.sorted())
    }

    func testSignedOutUserSignsInThenSeesTheFeed() {
        let app = launch(signedIn: false)

        let signIn = app.buttons["signin.button"]
        XCTAssertTrue(signIn.waitForExistence(timeout: 10))
        XCTAssertTrue(app.staticTexts["Connecte-toi avec ton compte Google pour voir le fil de tes abonnements."].exists)
        signIn.tap()

        XCTAssertTrue(app.staticTexts["Un long format sur Kotlin"].waitForExistence(timeout: 10))
    }

    func testSigningOutReturnsToTheSignInScreen() {
        let app = launch(signedIn: true)
        XCTAssertTrue(app.staticTexts["Un long format sur Kotlin"].waitForExistence(timeout: 10))

        app.buttons["feed.signout"].tap()

        XCTAssertTrue(app.buttons["signin.button"].waitForExistence(timeout: 10))
        XCTAssertFalse(app.staticTexts["Un long format sur Kotlin"].exists)
    }
}
