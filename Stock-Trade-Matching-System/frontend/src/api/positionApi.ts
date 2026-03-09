import { http } from "@/api/http";

// 定义持仓数据的长相（对应后端的 PositionEntity）
export interface Position {
  id: number;
  shareHolderId: string;
  market: string;
  securityId: string;
  totalQty: number;
  availableQty: number;
  frozenQty: number;
  updatedAt: string;
}

// 向后端发送 GET 请求获取当前用户的持仓
export async function getMyPositions(): Promise<Position[]> {
  // 注意：因为 http.ts 里已经配置了 baseURL: '/api'，所以这里直接写 '/positions' 即可
  const { data } = await http.get<Position[]>("/positions");
  return data;
}
