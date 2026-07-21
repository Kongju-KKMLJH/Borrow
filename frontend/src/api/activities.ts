import { activities, myCreated, myJoined } from '@/data/mock';
import type { Activity, FieldType } from '@/data/types';

import { simulate } from './client';

export type ActivityFilter = { type?: 'hobby' | 'class'; field?: FieldType; query?: string };

/** GET /activities */
export function listActivities(filter?: ActivityFilter): Promise<Activity[]> {
  let list = activities;
  if (filter?.type) list = list.filter((a) => a.type === filter.type);
  if (filter?.field) list = list.filter((a) => a.field === filter.field);
  if (filter?.query) list = list.filter((a) => a.title.includes(filter.query!));
  return simulate(list);
}

/** GET /activities/:id */
export function getActivity(id: string): Promise<Activity | undefined> {
  return simulate(activities.find((a) => a.id === id));
}

/** GET /me/activities */
export function getMyActivities(): Promise<{ joined: Activity[]; created: Activity[] }> {
  return simulate({ joined: myJoined, created: myCreated });
}

/** POST /activities — 개최 요청 생성. 생성 직후 상태는 '공간 승인 대기'. */
export function createActivity(payload: Partial<Activity>): Promise<Activity> {
  return simulate({ ...activities[0], ...payload, id: `a${Date.now()}`, status: 'pending' } as Activity);
}

/** POST /activities/:id/join */
export function joinActivity(id: string): Promise<{ ok: true }> {
  return simulate({ ok: true });
}
