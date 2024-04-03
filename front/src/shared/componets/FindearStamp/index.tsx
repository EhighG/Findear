import { Findear, Text, cls } from "@/shared";

type StampProps = {
  className?: string;
};

const FindearStamp = ({ className }: StampProps) => {
  return (
    <div
      className={cls(
        className ? className : "",
        "flex flex-col items-center border border-A706Dark rounded-full p-2 opacity-60 bg-A706Yellow rotate-45 absolute size-[70px]"
      )}
    >
      <>
        <img src={Findear} alt="Findear" className="size-[35px]" />
        <Text className="text-[0.8rem]">인계 완료</Text>
      </>
    </div>
  );
};

export default FindearStamp;
