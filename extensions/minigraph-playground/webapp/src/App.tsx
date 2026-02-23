import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Playground from './components/Playground';
import { PLAYGROUND_CONFIGS } from './config/playgrounds';

/**
 * To add a new playground tool, edit src/config/playgrounds.js only —
 * no changes needed here. Routes are generated automatically from the config.
 */
export default function App() {
  const defaultPath = PLAYGROUND_CONFIGS[0].path;

  return (
    <BrowserRouter>
      <Routes>
        {/* Redirect root to the first configured playground */}
        <Route path="/" element={<Navigate to={defaultPath} replace />} />

        {/* One route per configured playground */}
        {PLAYGROUND_CONFIGS.map((cfg) => (
          <Route key={cfg.path} path={cfg.path} element={<Playground config={cfg} />} />
        ))}

        {/* Fallback: unknown routes go to the first playground */}
        <Route path="*" element={<Navigate to={defaultPath} replace />} />
      </Routes>
    </BrowserRouter>
  );
}
