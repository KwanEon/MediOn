import { useEffect } from 'react';
import RoutesSetup from './routes/RoutesSetup';
import { useAuthStore } from './store/useAuthStore';

function App() {
  const loadCurrentUser = useAuthStore((state) => state.loadCurrentUser);

  useEffect(() => {
    void loadCurrentUser();
  }, [loadCurrentUser]);

  return <RoutesSetup />;
}

export default App;
