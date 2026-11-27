import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  {
    ignores: [
      '**/*.d.ts',
      'dist',
      'node_modules',
      'playwright-report',
      'test-results',
      'src/components/ui',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // Équivalent des arguments nommés : au plus un paramètre, donc un objet { champ: valeur }.
      'max-params': ['error', 1],
      'no-restricted-syntax': [
        'error',
        {
          selector: 'JSXText[value=/[A-Za-zÀ-ÿ]{2,}/]',
          message: 'Aucun texte en dur : utiliser src/strings.ts.',
        },
      ],
    },
  },
  { files: ['*.config.{ts,js}', 'e2e/**'], languageOptions: { globals: globals.node } },
)
