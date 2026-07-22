import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * 로그인 없이 X-Guest-Id 헤더로 사용자를 식별한다(백엔드 공통 규약).
 * 최초 실행 시 UUID를 생성해 기기에 저장하고, 이후에는 재사용한다.
 */
const STORAGE_KEY = 'borrow.guestId';

let cached: string | null = null;

function uuidv4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export async function getGuestId(): Promise<string> {
  if (cached) return cached;
  const stored = await AsyncStorage.getItem(STORAGE_KEY);
  if (stored) {
    cached = stored;
    return stored;
  }
  const id = uuidv4();
  await AsyncStorage.setItem(STORAGE_KEY, id);
  cached = id;
  return id;
}
