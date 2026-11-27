import Foundation

/// Destinations poussées dans la pile de navigation du fil (navigation typée, jamais de chaîne).
enum Route: Hashable {
    case player(videoId: String)
}
