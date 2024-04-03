from django.shortcuts import render
from django.http import JsonResponse
import json
import pandas as pd
from openai import OpenAI
from dotenv import load_dotenv
import os
import time
import re
from . import matching
import logging
import pickle
from .models import matchModel

logger = logging.getLogger(__name__)
# Create your views here.

matchInstance = matchModel()

def save_color(request):
    with open('colorDict.picklie', 'wb') as f:
        pickle.dump(matchInstance.colorDict, f)
    return JsonResponse({"saved"}, status = 200)

def health(request):
    return JsonResponse({"STATUS":"UP"}, status = 200)

# lost112 매칭 요청 처리 함수
def lost_matching(request):
    logger.warning(json.loads(request.body))
    if request.method == 'POST':
        try:
            body = json.loads(request.body.decode('utf-8'))
        except json.JSONDecodeError:
            return JsonResponse({'error':'invalid json'}, status=400)
        acquiredBoardList = body.get("acquiredBoardList")
        if acquiredBoardList:
            result = process_lost_item_data(body)
        else:  # 습득물 리스트가 없을 경우
            return JsonResponse({'message':'해당 분실물과 매칭 가능한 lost112 데이터가 없습니다.', 'result':None}, status = 200)
    else:  # post 요청이 아닐 경우
        return JsonResponse({'error': 'Only POST requests are allowed'}, status=405)
   
    return JsonResponse({ 'message':'해당 분실물과 lost112 데이터와의 매칭이 완료되었습니다', 'result':result }, status = 200)

# lost112 매칭 실행 함수
def process_lost_item_data(items_info):
    # 코드 실행 시작 시간 측정
    start_time = time.time()
    
    lostBoard = items_info["lostBoard"]
    acquiredBoardList = items_info.get("acquiredBoardList")
    
    # 데이터 처리 수행
    matchInstance.setData(lost=lostBoard, found=acquiredBoardList)
    matchInstance.preprocess('lost112')

    matchInstance.calColor()
    matchInstance.calDistance()
    matchInstance.calName()
    processed_data = matchInstance.aggregateScore()
    
    # 코드 실행 종료 시간 측정
    end_time = time.time()

    # 시작 시간과 종료 시간의 차이를 계산하여 실행 시간 출력
    execution_time = end_time - start_time
    print(f"실행 시간: {execution_time} 초")
    
    return processed_data


# findear 매칭 요청 처리 함수
def findear_matching(request):
    logger.warning(json.loads(request.body))
    if request.method == 'POST':
        try:
            body = json.loads(request.body.decode('utf-8'))
        except json.JSONDecodeError:
            return JsonResponse({'error':'invalid json'}, status=400)
        acquiredBoardList = body.get("acquiredBoardList")
        if acquiredBoardList:
            result = process_findear_item_data(body)
        else:  # 습득물 리스트가 없을 경우
            return JsonResponse({'message':'해당 분실물과 매칭 가능한 findear 데이터가 없습니다.', 'result':None}, status = 200)
    else:  # post 요청이 아닐 경우
        return JsonResponse({'error': 'Only POST requests are allowed'}, status=405)
   
    return JsonResponse({ 'message':'해당 분실물과 findear 데이터와의 매칭이 완료되었습니다', 'result':result }, status = 200)

# findear 매칭 실행 함수
def process_findear_item_data(items_info):
    # 코드 실행 시작 시간 측정
    start_time = time.time()
    
    lostBoard = items_info["lostBoard"]
    acquiredBoardList = items_info.get("acquiredBoardList")
    
    # 데이터 처리 수행
    matchInstance.setData(lost=lostBoard, found=acquiredBoardList)
    matchInstance.preprocess('findear')

    matchInstance.calColor()
    matchInstance.calDistance()
    matchInstance.calName()
    matchInstance.calDesc()
    processed_data = matchInstance.aggregateScore()
    
    # 코드 실행 종료 시간 측정
    end_time = time.time()

    # 시작 시간과 종료 시간의 차이를 계산하여 실행 시간 출력
    execution_time = end_time - start_time
    logger.warning(f"실행 시간: {execution_time} 초")
    return processed_data

# 게시글 이미지 정보 추출
def image_process(request):
    if request.method == 'POST':
        try:
            body = json.loads(request.body)
        except json.JSONDecodeError:  # body 데이터가 json이 아닐 경우
            return JsonResponse({'error':'invalid json'}, status=400)
        if body.get("productName") and body.get("imgUrl"):  
            product_name = body["productName"]
            image_url = body["imgUrl"]
            result = process_execute(product_name, image_url)
        else:  # 적절한 body 데이터가 아닐 경우
            return JsonResponse({'error': 'Incorrect request body'}, status=400)
    else:  # post 요청이 아닐 경우
        return JsonResponse({'error': 'Only POST requests are allowed'}, status=405)
    if not result:  
        return JsonResponse({'message':'GPT api failed'}, status = 404)
    return JsonResponse({ 'message':'success', 'result':result }, status = 200)
    

def process_execute(product_name, image_url):
    # 코드 실행 시작 시간 측정
    start_time = time.time()

    load_dotenv()

    client = OpenAI(
        api_key=os.environ.get("OPENAI_API_KEY"),
    )
    
    question_text = f"""
    분실물에 대한 여러 정보를 제공하려고 해.
    물품명은 '{product_name}'야.
    사진 속 가장 중심이 되는 물체를 물품명을 포함해 분석하여 다음의 형식에 맞춰 제공해줘.

    - 물체의 카테고리(카드, 지갑, 현금, 의류, 전자기기, 가방, 휴대폰, 증명서, 쇼핑백, 귀금속, 유가증권, 자동차, 서류, 도서용품, 스포츠용품, 컴퓨터, 산업용품, 악기, 기타 중 하나)
    - 물체의 색상 1가지(검정, 흰, 빨강, 오렌지, 노랑, 초록, 파랑, 갈, 보라, 회, 기타 중 하나)
    - 물체 외형 특징에 대한 추가 키워드 5개
    {{
        "category" : "카테고리명",
        "color" : "색상명"
        "description" : ["키워드1", "키워드2", ... ]
    }}
    """
    try:
        response = client.chat.completions.create(
            model="gpt-4-vision-preview",
            messages=[
                {"role": "system", "content": "You are a helpful assistant designed to output JSON. Give only JSON"},
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": question_text},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": image_url,
                                "detail": "low"
                            }
                        },
                    ],
                }
            ],
            max_tokens=300,
        )
    except Exception as e:  # 모델에서 에러 날 경우 예외처리
        error_message = f"GPT 모델에 요청 중 오류: {str(e)}"
        result = False
        print(error_message)
        return result
    # gpt 답변 중 내용만 추출
    gpt_content = response.choices[0].message.content
    # usage_token = response['usage']['total_tokens']
    print("내용 : ", gpt_content)

    # 내용 중 중괄호 사이의 문자열 추출
    match = re.search(r'{(.+)}', gpt_content, re.DOTALL)
    result_data = json.loads(match.group(0))
    print(result_data)
    print(type(result_data))

    # 코드 실행 종료 시간 측정
    end_time = time.time()

    # 시작 시간과 종료 시간의 차이를 계산하여 실행 시간 출력
    execution_time = end_time - start_time
    print(f"실행 시간: {execution_time} 초")
    return result_data