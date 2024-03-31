interface Member {
  memberId: number;
  phoneNumber: string;
  role: string;
}

interface Agency {
  id: number;
  name: string;
  address: string;
}

type postionType = {
  xPos: number;
  yPos: number;
};

type dataType = {
  address_name: string;
  category_group_code: string;
  category_group_name: string;
  category_name: string;
  distance: string;
  id: string;
  phone: string;
  place_name: string;
  place_url: string;
  road_address_name: string;
  x: string;
  y: string;
};

export type { Member, Agency, postionType, dataType };
