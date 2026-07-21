import * as ImageManipulator from 'expo-image-manipulator';

import type { ApiResponse } from '@/data/types';
import { getGuestId } from '@/lib/guest';

import { API_BASE_URL } from './client';

/** 업로드 전 리사이즈·압축 (카메라 원본 수 MB → 413 방지). */
async function compress(uri: string): Promise<string> {
  const out = await ImageManipulator.manipulateAsync(
    uri,
    [{ resize: { width: 1280 } }],
    { compress: 0.8, format: ImageManipulator.SaveFormat.JPEG },
  );
  return out.uri;
}

/**
 * 로컬 이미지 URI들을 서버(POST /api/uploads, multipart)에 올리고
 * 상대경로 URL 배열(["/files/a.jpg", ...])을 받는다. 이 값을 등록 요청의 imageUrls로 그대로 전달한다.
 */
export async function uploadImages(uris: string[]): Promise<string[]> {
  if (uris.length === 0) return [];
  const guestId = await getGuestId();
  const form = new FormData();
  for (const uri of uris) {
    const small = await compress(uri);
    // 파트 이름은 반드시 "files" (백엔드 @RequestPart("files")). Content-Type은 지정하지 않는다(RN이 boundary 설정).
    form.append('files', { uri: small, name: 'photo.jpg', type: 'image/jpeg' } as unknown as Blob);
  }
  const res = await fetch(`${API_BASE_URL}/api/uploads`, {
    method: 'POST',
    headers: { 'X-Guest-Id': guestId },
    body: form,
  });
  const json = (await res.json()) as ApiResponse<{ urls: string[] }>;
  if (!json.success) throw new Error(json.error?.message ?? '이미지 업로드에 실패했어요.');
  return json.data.urls;
}
