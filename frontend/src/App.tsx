import { useEffect } from 'react';
import RoutesSetup from './routes/RoutesSetup';
import { useAuthStore } from './store/useAuthStore';
import { useMedicalSearchStore } from './store/useMedicalSearchStore';

function App() {
  const loadCurrentUser = useAuthStore((state) => state.loadCurrentUser);
  const authInitialized = useAuthStore((state) => state.initialized);
  const userId = useAuthStore((state) => state.user?.id ?? null);
  const loadFavorites = useMedicalSearchStore((state) => state.loadFavorites);
  const clearFavorites = useMedicalSearchStore((state) => state.clearFavorites);

  useEffect(() => {
    void loadCurrentUser();
  }, [loadCurrentUser]);

  useEffect(() => {
    if (!authInitialized) {
      return;
    }
    if (userId === null) {
      clearFavorites();
      return;
    }
    void loadFavorites(userId);
  }, [authInitialized, clearFavorites, loadFavorites, userId]);

  return <RoutesSetup />;
}

export default App;
