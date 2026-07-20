import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { syncState } from '../redux/interviewSlice';

export const useTabSync = () => {
    const dispatch = useDispatch();

    useEffect(() => {
        const handleStorageChange = (event) => {
            if (event.key === 'interviewState' && event.newValue) {
                try {
                    const newState = JSON.parse(event.newValue);
                    dispatch(syncState(newState));
                } catch (error) {
                    console.error("Failed to parse state from storage", error);
                }
            }
        };

        window.addEventListener('storage', handleStorageChange);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
        };
    }, [dispatch]);
};