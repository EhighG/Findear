import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import { CustomButton, Text, useMemberStore } from "@/shared";
import { Breadcrumb, FloatingLabel, Label, Modal } from "flowbite-react";
import { useEffect, useState } from "react";
import { getAcquisitionsDetail, returnAcquisitions } from "@/entities";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import {
  cancelScarppedBoard,
  scrapBoard,
  receiverType,
} from "@/entities/findear/api";

// type board = {
//   boardId: number;
//   memberId: number;
//   productName: string;
//   description: string;
//   status: string;
//   deleteYn: boolean;
//   registeredAt?: string;
//   thumbnailUrl: string;
//   categoryName: string;
//   color: string;
// };

// type AcquiredBoard = {
//   acquiredBoardId: number;
//   address: string;
//   name: string;
//   x_pos: number;
//   y_pos: number;
// };

const foundItemDetail = () => {
  const navigate = useNavigate();

  const { member } = useMemberStore();

  const [category, setCategory] = useState<string>("지갑");
  const [founderId, setFounderId] = useState<string>("ztrl");
  const [imageUrlList, setImageUrlList] = useState<string[]>([
    "../images/wallet.jpg",
  ]);
  // const [color, setColor] = useState<string>("빨간색");
  const [productName, setProductName] = useState<string>("갈색 지갑");
  const [description, setDescription] =
    useState<string>("신분증이 든 갈색 지갑입니다.");
  const [address, setAddress] = useState<string>("서울시 강남구 역삼동");
  const [acquiredAt, setAcquiredAt] = useState<string>("2024.03.26 17:31:00");
  const [placeName, setPlaceName] = useState<string>("멀티캠퍼스");
  const [isScrapped, setScrapped] = useState<boolean>(false);
  const [isReturned, setReturned] = useState<boolean>(false);

  const boardId = parseInt(useParams().id ?? "0");
  const [query] = useSearchParams();
  const [receiver, setReceiver] = useState<receiverType>();
  const [receiverName, setReceiverName] = useState<string>();
  const [receiverEmail, setReceiverEmail] = useState<string>();
  const [receiverPhoneNumber, setReceiverPhoneNumber] = useState<string>();

  const [modalOptions, setModalOptions] = useState<boolean>(false);

  const initReceiver = () => {
    if (
      query &&
      query.get("name") &&
      query.get("email") &&
      query.get("phoneNumber")
    ) {
      setReceiver({
        name: query.get("name") ?? "",
        email: query.get("email") ?? "",
        phoneNumber: query.get("phoneNumber") ?? "",
      });
    }
  };

  initReceiver();

  getAcquisitionsDetail(
    boardId,
    ({ data }) => {
      console.log(data);
      setFounderId(data.result.acquisitionId);
      // setColor(data.result.color);
      setProductName(data.result.productName);
      setDescription(data.result.description);
      setCategory(data.result.categoryName);
      setImageUrlList(data.result.imgUrlList);
      setAcquiredAt(data.result.acquiredAt);
      setAddress(data.result.address);
      setPlaceName(data.result.name);
    },
    (error) => console.log(error)
  );

  const returnItem = () => {
    if (!receiver) {
      alert("인계 대상자 정보를 입력해주세요.");
    } else {
      returnAcquisitions(
        { boardId, receiver },
        ({ data }) => {
          console.log(data);
          alert(data.message);
          setReturned(true);
        },
        (error) => {
          alert(error.message);
          setReturned(false);
        }
      );
      setModalOptions(false);
    }
  };

  const sendMessage = () => {
    navigate("/letter", { state: { key: founderId } });
  };

  const scarpItem = (scrpping: boolean) => {
    if (scrpping) {
      scrapBoard(
        boardId,
        ({ data }) => {
          alert(data.message);
          setScrapped(scrpping);
        },
        (error) => {
          console.log(error);
          alert(error.message);
        }
      );
    } else {
      cancelScarppedBoard(
        boardId,
        ({ data }) => {
          alert(data.message);
          setScrapped(scrpping);
        },
        (error) => {
          console.log(error);
          alert(error.message);
        }
      );
    }
  };

  const checkReturnState = (isReturned: boolean) => {
    return isReturned ? (
      <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706DarkGrey1 text-white">
        <p>인계 완료</p>
      </CustomButton>
    ) : (
      <CustomButton
        className="rounded-md w-[320px] h-[60px] bg-A706CheryBlue text-white"
        onClick={() => setModalOptions(true)}
      >
        <p>인계하기</p>
      </CustomButton>
    );
  };

  const showSendButton = (role: string) => {
    return role === "NORMAL" ? (
      <CustomButton
        className="main-nav-button"
        onClick={() => {
          sendMessage();
        }}
      >
        <>
          <SendOutlinedIcon />
          쪽지 보내기
        </>
      </CustomButton>
    ) : (
      <></>
    );
  };

  const showScrapbutton = (isScrapped: boolean) => {
    return isScrapped ? (
      <CustomButton
        className="main-nav-button"
        onClick={() => scarpItem(false)}
      >
        <>
          <FavoriteIcon />
          <p>스크랩 완료</p>
        </>
      </CustomButton>
    ) : (
      <CustomButton
        className="main-nav-button"
        onClick={() => {
          scarpItem(true);
        }}
      >
        <>
          <FavoriteBorderOutlinedIcon />
          <p>스크랩하기</p>
        </>
      </CustomButton>
    );
  };

  useEffect(() => {
    if (receiverName && receiverEmail && receiverPhoneNumber) {
      setReceiver({
        name: receiverName,
        email: receiverEmail,
        phoneNumber: receiverPhoneNumber,
      });
    }
  }, [receiverName, receiverEmail, receiverPhoneNumber]);

  return (
    <>
      <div className="flex flex-col justify-center items-center p-[40px]">
        <div className="flex flex-row justify-between w-[340px] mb-10">
          <Breadcrumb>
            <Breadcrumb.Item>카테고리</Breadcrumb.Item>
            <Breadcrumb.Item>{category}</Breadcrumb.Item>
          </Breadcrumb>
          <Text className="text-sm">{"습득자: " + founderId}</Text>
        </div>
        <div className="flex flex-col justify-center p-[40px]">
          <div className="flex justify-center mx-5">
            <img
              src={imageUrlList[0]}
              alt="이미지가 없습니다."
              className="size-[170px] self-center"
            />
          </div>
        </div>
        <div className="w-[340px] flex flex-col text-end">
          <p className="text-sm">{address + ", " + placeName}</p>
          <p className="text-xs">{acquiredAt}</p>
        </div>
        <div className="flex flex-row justify-between mt-10 w-[340px]">
          <div className="w-full">
            <Label color="secondary" value="이름" />
            <Text children={productName} className="text-lg font-bold" />
            <div className="h-[1px] bg-A706DarkGrey2"></div>
          </div>
          {/* <div className="flex justify-start">
            <ColorLensIcon />
            <Text className="mx-5">{color}</Text>
          </div> */}
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="설명" />
          <div className="bg-white shadow-md min-h-[50px] p-5 rounded-md">
            {description}
          </div>
        </div>
        {/* <div className="w-[340px] mt-10">
          <Label color="secondary" value="장소 이름" />
          <Text children={placeName} className="w-[340px] text-lg font-bold" />
          <div className="h-[1px] bg-A706DarkGrey2"></div>
        </div> */}
        <div className="w-[340px] mt-10 flex flex-row justify-around">
          {showSendButton(member.role)}
          {member.role === "NORMAL"
            ? showScrapbutton(isScrapped)
            : checkReturnState(isReturned)}
        </div>
        <Modal
          dismissible
          show={modalOptions}
          position={"center"}
          size="md"
          onClose={() => {
            setModalOptions(false);
          }}
        >
          <Modal.Header>
            <p>인계 대상자 정보</p>
          </Modal.Header>
          <Modal.Body className="flex flex-col justify-center">
            <FloatingLabel
              variant="outlined"
              label="이름을 입력해 주세요."
              sizing="sm"
              defaultValue={receiverName}
              onChange={(e) => setReceiverName(e.target.value)}
            />
            <FloatingLabel
              variant="outlined"
              label="이메일을 입력해 주세요."
              sizing="sm"
              defaultValue={receiverEmail}
              onChange={(e) => setReceiverEmail(e.target.value)}
            />
            <FloatingLabel
              variant="outlined"
              label="연락처를 입력해 주세요."
              sizing="sm"
              defaultValue={receiverPhoneNumber}
              onChange={(e) => setReceiverPhoneNumber(e.target.value)}
            />
            <CustomButton
              className="rounded-md w-full h-[60px] bg-A706CheryBlue text-white self-center"
              onClick={() => returnItem()}
            >
              <p>인계</p>
            </CustomButton>
          </Modal.Body>
        </Modal>
      </div>
    </>
  );
};

export default foundItemDetail;
