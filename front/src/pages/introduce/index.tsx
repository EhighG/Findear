import { BentoCard, Text, handAcquire, registAcquire } from "@/shared";
import { Helmet } from "react-helmet-async";
import Swal from "sweetalert2";
const Introduce = () => {
  return (
    <div className="flex flex-col flex-1 p-10 justify-center">
      <Helmet>
        <title>파인디어 소개 페이지</title>
        <meta name="description" content="파인디어 팁, 소개, 안내 페이지" />
        <meta
          name="keywords"
          content="Findear, 파인디어, 팁, 소개, 안내, introduce, tip"
        />
      </Helmet>
      <div className="grid max-sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-3 place-content-center align-baseline ">
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "습득물, 분실물",
              text: "습득물은 시설관리자가 습득해 보관중인 물건을 의미하며, 분실물은 잃어버린 물건 자체를 의미합니다.",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">습득물, 분실물?</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People%20with%20activities/Woman%20Shrugging%20Light%20Skin%20Tone.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "파인디어",
              text: "파인디어는 분실물과 습득물을 쉽게 등록하고 관리하고 매칭까지 도와주는 통합 분실물 관리 플랫폼입니다.",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">Findear</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People%20with%20professions/Detective%20Medium-Light%20Skin%20Tone.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "경찰청 자료도 파인디어!",
              text: "Findear에서는 경찰청에서 통합 관리중인 습득물 데이터도 확인 할 수 있어요",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">Lost112</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People%20with%20professions/Woman%20Police%20Officer%20Light%20Skin%20Tone.png"
              alt="Lost112"
              className="size-[100px]"
            />
          </div>
        </BentoCard>

        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "습득물 등록",
              text: "보관중인 습득물의 사진과 물건명만 입력하세요, AI가 카테고리, 색상, 브랜드 등 상세 정보를 자동으로 등록해드리고 습득물 관리 기능도 제공합니다.",
              imageUrl: registAcquire,
              imageWidth: 300,
              imageHeight: 300,
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">습득물 등록</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People%20with%20activities/Person%20Tipping%20Hand%20Light%20Skin%20Tone.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "습득물 관리",
              text: "Findear에서는 시설관리자 분들이 불편하지 않게 습득물 인계, 폐기알림, 보관중인 습득물 조회 등 다양한 관리 기능을 제공합니다.",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">습득물 관리</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People/Man%20Genie.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "습득물 통합 조회",
              text: "전국 경찰서, 공공시설을 포함한 모든 시설관리자들이 관리중인 습득물을 파인디어에서 조회할 수 있어요!",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">습득물 조회</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Cat%20Face.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "습득물 인계",
              text: "시설 관리자님이라면 Findear에서 쉽고 안전하게 습득물 인계를 진행할 수 있습니다. 상대방 전화번호를 입력하고 Findear 회원인증 문구만 확인하고 인계하세요!, Findear 회원은 본인인증이 완료된 사용자입니다.",
              imageUrl: handAcquire,
              imageWidth: 300,
              imageHeight: 300,
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">습득물 인계절차</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Folded%20Hands.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "분실물 등록",
              text: "물건을 잃어버리셨다면 반드시 Findear에 분실물 정보를 등록해주세요! 등록하신 분실물 정보는 모든 Findear 사용자가 확인할 수 있고 매칭도 진행해드립니다.",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">분실물 등록</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People/Man%20Frowning.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
        <BentoCard
          onClick={() =>
            Swal.fire({
              title: "유사 습득물 매칭",
              text: "등록하신 분실물 정보와 Findear에 등록된 습득물 정보를 비교하여 유사한 물건이 있다면 매칭 및 알림을 드립니다, 매칭 기능을 위해 핸드폰 바탕화면에 Findear 바로가기를 만들어주세요!",
              confirmButtonText: "확인",
            })
          }
        >
          <Text className="text-[28px] font-bold">유사 습득물 매칭</Text>
          <div className="flex justify-center">
            <img
              src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/People/Superhero.png"
              alt="Findear"
              className="size-[100px]"
            />
          </div>
        </BentoCard>
      </div>
    </div>
  );
};

export default Introduce;
