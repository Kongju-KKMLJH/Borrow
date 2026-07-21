import { providerSummary, requests } from '@/data/mock';
import type { HostingRequest, RequestStatus } from '@/data/types';

import { simulate } from './client';

/** GET /provider/requests?status= */
export function listRequests(status?: RequestStatus): Promise<HostingRequest[]> {
  return simulate(status ? requests.filter((r) => r.status === status) : requests);
}

/** GET /provider/requests/:id */
export function getRequest(id: string): Promise<HostingRequest | undefined> {
  return simulate(requests.find((r) => r.id === id));
}

/** POST /provider/requests/:id/approve */
export function approveRequest(id: string): Promise<{ ok: true }> {
  return simulate({ ok: true });
}

/** POST /provider/requests/:id/reject */
export function rejectRequest(id: string, reason: string): Promise<{ ok: true }> {
  return simulate({ ok: true });
}

/** GET /provider/summary */
export function getProviderSummary(): Promise<typeof providerSummary> {
  return simulate(providerSummary);
}
