/**
 * Conectivos de nomes pessoais em português (e alguns estrangeiros comuns)
 * que devem ser ignorados ao escolher primeiro/último nome.
 */
const CONECTIVOS = new Set([
  "de", "da", "do", "das", "dos",
  "e",
  "di", "del", "della", "dello",
  "van", "von", "der", "den",
  "la", "le", "lo",
])

/**
 * Encurta um nome para "Primeiro + Último" ignorando conectivos.
 *
 * Exemplos:
 *  - "Maria"                                 -> "Maria"
 *  - "Maria Silva"                           -> "Maria Silva"
 *  - "Maria das Graças Silva"                -> "Maria Silva"
 *  - "Clara Tupinambá Torres de Almeida"     -> "Clara Almeida"
 *  - "Tatiana Vivas da Silva"                -> "Tatiana Silva"
 *
 * Se o nome estiver vazio ou for apenas conectivos, retorna o original.
 */
export function shortenName(fullName: string | null | undefined): string {
  if (!fullName) return ""
  const partes = fullName.trim().split(/\s+/)
  if (partes.length === 0) return ""

  const significativas = partes.filter(p => !CONECTIVOS.has(p.toLowerCase()))
  if (significativas.length === 0) return fullName.trim()
  if (significativas.length === 1) return significativas[0]

  const primeiro = significativas[0]
  const ultimo = significativas[significativas.length - 1]
  return `${primeiro} ${ultimo}`
}
