import type { Member } from "@/entities";

type boardType = {
  id: number;
  color?: string;
  description?: string;
  categoryName?: string;
  productName: string;
  registeredAt: string;
  imgUrls: string[];
  isLost: boolean;
  member: Member; // memberType
};

type infoType = {
  address: string;
  agencyName: string;
  id: number;
  xpos: number;
  ypos: number;
  board: boardType;
};

export type { infoType };
