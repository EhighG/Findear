import { BentoCard, Text } from "@/shared";
import { Lost112, boardImage3 } from "@/shared";
const Introduce = () => {
  return (
    <div className="flex flex-col flex-1 p-10 justify-center">
      <div className="grid grid-flow-row-dense sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 ">
        <BentoCard className="col-span-1 row-span-1">
          <Text className="text-[28px] font-bold">Lost112</Text>
          <div className="flex justify-center">
            <Lost112 width="100" height="100" />
          </div>
        </BentoCard>
        <BentoCard className="col-span-full row-span-2">
          <Text className="text-[28px] font-bold">Findear</Text>
          <div className="flex justify-center">
            <img src={boardImage3} alt="Findear" className="size-[100px]" />
          </div>
        </BentoCard>

        <BentoCard className="col-span-1">
          <Text className="text-[28px] font-bold">습득물 조회</Text>
        </BentoCard>
        <BentoCard className="col-span-1">
          <Text className="text-[28px] font-bold">습득물 등록</Text>
        </BentoCard>
        <BentoCard className="col-span-1">
          <Text className="text-[28px] font-bold">습득물 인계절차</Text>
        </BentoCard>
        <BentoCard className="col-span-1">
          <Text className="text-[28px] font-bold">분실물 등록</Text>
        </BentoCard>
        <BentoCard className="col-span-1">
          <Text className="text-[28px] font-bold">유사 습득물 추천</Text>
        </BentoCard>
      </div>
    </div>
  );
};

export default Introduce;
