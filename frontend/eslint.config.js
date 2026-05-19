import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    rules: {
      // Regras do React Compiler (eslint-plugin-react-hooks 7) sao muito
      // estritas para aplicar retroativamente. O codigo funciona; mantidas
      // como "warn" para ficarem visiveis sem quebrar o build:
      //  - purity: uso de Date.now() no render (calculo de idade/dias)
      //  - set-state-in-effect: reset de formularios em useEffect (padrao comum)
      'react-hooks/purity': 'warn',
      'react-hooks/set-state-in-effect': 'warn',
    },
  },
  {
    // Componentes shadcn/ui sao copiados de terceiros — exportam variantes
    // (buttonVariants, badgeVariants...) junto com o componente, e o
    // use-toast usa um objeto so como fonte de tipos.
    files: ['src/components/ui/**/*.{ts,tsx}', 'src/hooks/**/*.{ts,tsx}'],
    rules: {
      'react-refresh/only-export-components': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
    },
  },
])
