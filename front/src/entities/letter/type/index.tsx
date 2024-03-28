type roomDetailType = {
  board: boardType;
  message: messageType;
};

type boardType = {
  boardId: number;
  thumbnailUrl: string;
  productName: string;
  description: string;
};

type roomListType = {
  boardId: number;
  content: string;
  description?: string;
  messageRoomId: number;
  productName: string;
  thumbnailUrl: string;
  title: string;
  sendAt: Date;
};

type messageType = {
  messageId: number;
  messageRoomId: number;
  title: string;
  content: string;
  sendAt: string;
  senderId: number;
};

export type { roomListType, roomDetailType };
