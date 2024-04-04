from django.db import models

import pandas as pd
import numpy as np
import fasttext
from sklearn.preprocessing import MinMaxScaler
import haversine.haversine as hv
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from kiwipiepy import Kiwi
import re
from dotenv import load_dotenv
import os
import time
import logging
import pickle

from selenium import webdriver 
from selenium.webdriver.common.by import By # find_element 함수 쉽게 쓰기 위함
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import Select
import random
# Create your models here.

logger = logging.getLogger(__name__)
class matchModel():
    
    def logging_time(original_fn):
        def wrapper_fn(*args, **kwargs):
            start_time = time.time()
            result = original_fn(*args, **kwargs)
            end_time = time.time()
            print("WorkingTime[{}]: {} sec".format(original_fn.__name__, end_time-start_time))
            return result
        return wrapper_fn

    def __init__(self) -> None:
        # load model
        load_dotenv()
        path = os.getenv("MODEL_PATH")
        self.model = fasttext.load_model(path)

        # load kiwi
        self.kiwi = Kiwi()
        self.stem_tag = ['NNG', 'NNP', 'VA'] 
        
        # load police address coordinate data
        coordinate_path = os.getenv("COORDINATE_PATH")
        self.location_df = pd.read_csv(coordinate_path)

        # open color crawling
        self.webPath = 'http://web.kats.go.kr/KoreaColor/color.asp'
        options = webdriver.ChromeOptions()
        options.add_argument('--headless')
        options.add_argument("--disable-gpu")
        options.add_argument("--no-sandbox")
        options.add_argument("start-maximized")
        options.add_argument("enable-automation")
        options.add_argument("--disable-infobars")
        options.add_argument("--disable-dev-shm-usage")

        self.driver = webdriver.Chrome(options=options)
        self.driver.get(self.webPath)
        self.timeToWait = 0.00001

        # open color cache file
        if 'colorDict.pickle' in os.listdir('.'):
            with open('colorDict.pickle', 'rb') as f:
                self.colorDict = pickle.load(f)
                print(self.colorDict)
        else:
            self.colorDict = dict()
        return None

    def setData(self, lost, found):
        self.lost = lost
        self.found = found
        logger.debug(f'Get Data')
        logger.debug(f'lost :: {lost}')
        logger.debug(f'found :: {found}')

    def preprocess(self, source):
        if source not in ['lost112', 'findear']: 
            raise Exception('input wrong')
        self.source = source 
        # set lost item type
        self.lost['lostBoardId'] = int(self.lost['lostBoardId'])
        self.lost['xpos'] = float(self.lost['xpos'])
        self.lost['ypos'] = float(self.lost['ypos'])

        # set found item type
        self.found = pd.DataFrame(self.found)

        if self.source == 'findear':
            self.found['acquiredBoardId'] = self.found['acquiredBoardId'].astype(int)
            self.found['xpos'] = self.found['xpos'].astype(float)
            self.found['ypos'] = self.found['ypos'].astype(float)
        elif self.source == 'lost112':
            # 칼럼명 통일
            self.found = self.found.rename(columns={'id':'acquiredBoardId', 'fdPrdtNm':'productName', 'clrNm':'color', 'depPlace':'place'})  
            self.found['acquiredBoardId'] = self.found['acquiredBoardId'].astype(int)
            # 좌표 값 집어넣기
            self.found['xpos'] = 0.0
            self.found['ypos'] = 0.0
            
            # print(location_df.head())
            for index, row in self.found.iterrows():
                try:
                    place_name = row['place']
                    # print(place_name)
                    location_df_row = self.location_df[self.location_df['관서명'] == place_name]
                    # print(location_df_row['위도'].values[0])
                    xpos = location_df_row['Longitude'].values[0]
                    ypos = location_df_row['Latitude'].values[0]
                    self.found.at[index, 'xpos'] = xpos
                    self.found.at[index, 'ypos'] = ypos
                except:
                    continue
            print(self.found)

        self.score = pd.DataFrame()
        self.score['id'] = self.found['acquiredBoardId']

    def calColor(self):
        std = 100 
        self.score['color'] = 0
        lostColor = self.getColor(self.lost['color'])
        if lostColor is None: return None
        npLst = np.array([])
        for i in self.found['color']:
            foundColor = self.getColor(i)
            if foundColor is None:
                npLst = np.append(npLst,[std])
                continue
            diff = self.delta_E_CMC(lostColor, foundColor)
            npLst = np.append(npLst,[diff])
        #print('complete np', npLst )
        npLst = npLst.reshape(-1,1)
        minmaxScaler = MinMaxScaler().fit([[0],[std]])
        X_train_minmax = minmaxScaler.transform(npLst)
        nplst = 1 - np.array(X_train_minmax).squeeze()

        self.score['color'] = nplst
        return
    
    def calDistance(self):
        std = 20
        npLst = []

        if -90 < self.lost['ypos'] < 90 and -180 < self.lost['xpos'] < 180:  # 분실물 위경도 범위 확인
            for x,y in zip(self.found['xpos'], self.found['ypos']):
                if -90 < y < 90 and -180 < x < 180:  # 습득물 위경도 범위 확인
                    dist = hv((y,x), ( self.lost['ypos'],self.lost['xpos']), unit='km')
                    # print(dist)
                    npLst.append([dist])
                else:
                    npLst.append([1000])
            
        # npLst = np.array([[hv((y,x), ( self.lost['ypos'],self.lost['xpos']), unit='km')] for x,y in zip(self.found['xpos'], self.found['ypos']) ])
        
        minmaxScaler = MinMaxScaler().fit([[0],[std]])
        X_train_minmax = minmaxScaler.transform(npLst)
        nplst = 1- np.array(X_train_minmax).squeeze()
        self.score['place'] = nplst
        return None
    
    def calName(self):
        lostName = self.lost['productName'].replace(' ','')
        foundName = self.found['productName'].map(lambda x: x.replace(' ', '')).to_list()
        npLst = np.array([self.getCoSim(self.model[lostName], self.model[i]) for i in foundName])
        self.score['name'] = npLst
        return None
    
    def calDesc(self):
        foundToken = [self.getDocumentToken(document) for document in self.found['description']]
        print(foundToken)
        lostToken = self.getDocumentToken(self.lost['description']) 

        lostVector = self.getDocumentVector(lostToken)
        foundVector = [self.getDocumentVector(document) for document in foundToken]

        npLst = np.array([self.getCoSim(lostVector, i) for i in foundVector])
        self.score['desc'] = npLst
    
    def aggregateScore(self):

        self.score['mean_value'] = self.score.iloc[:, 1:].mean(axis=1)
        self.score['mean_value'] = self.score['mean_value'].round(decimals=5)
        
        lostBoardId = self.lost["lostBoardId"]
            
        if self.source == 'findear':
            # 평균 값을 기준으로 DataFrame 정렬
            df_sorted = self.score.sort_values(by='mean_value', ascending=False)
            print(df_sorted)
            # 데이터프레임 순회하며 반환 형태로 변환하는 리스트 컴프리헨션
            result_data = [
                {"lostBoardId": int(lostBoardId), "acquiredBoardId": int(row["id"]), "similarityRate": row["mean_value"]}
                for index, row in df_sorted.loc[:, ['id', 'mean_value']].iterrows()
            ]
            
        elif self.source == 'lost112':
            # 응답 데이터를 위한 df concat
            response_df = pd.concat([self.score, self.found], axis=1)
            # 평균 값을 기준으로 DataFrame 정렬
            df_sorted = response_df.sort_values(by='mean_value', ascending=False)
            print(df_sorted)
            # 데이터프레임 순회하며 반환 형태로 변환하는 리스트 컴프리헨션
            result_data = [
                {"lostBoardId": int(lostBoardId), 
                 "acquiredBoardId": int(row["id"]), 
                 "similarityRate": row["mean_value"],
                 "atcId": row["atcId"],
                 "depPlace": row["place"].values[1],
                 "fdFilePathImg": row["fdFilePathImg"],
                 "fdPrdtNm": row["productName"],
                 "fdSbjt": row["fdSbjt"],
                 "clrNm": row["color"].values[1],
                 "fdYmd": row["fdYmd"],
                 "mainPrdtClNm": row["mainPrdtClNm"]
                 }
                for index, row in df_sorted.loc[:, ('id', 'mean_value', "atcId", "place", "fdFilePathImg", "productName", "fdSbjt", "color", "fdYmd", "mainPrdtClNm")].iterrows()
            ]
        if len(result_data) > 100 : result_data = result_data[:100]
        logger.warn(f'응답: {result_data}')
        return result_data 

    def test(self):
        print('teststest')
        return 'test'

    def getDocumentVector(self, document):
        vector = np.array(sum(self.model[doc] for doc in document)) / len(document)
        return vector

    def getDocumentToken(self, document):
        charEngNum = re.findall(r'[A-Za-z0-9]+', document)
        meaningfulText = self.kiwi.tokenize(document)
        token = [t.form for t in meaningfulText if t.tag in self.stem_tag] + charEngNum
        return token

    @logging_time
    def getColor(self, query):
        '''
        None 반환 시 색 계산 제외
        '''
        tmpPkl = query
        if type(query) != str: return None
        if query in self.colorDict:
            return self.colorDict[query]
        if query == '':
            return None
        elif len(query) == 1:
            query = query+'색' 
        inputBox = self.driver.find_element(By.XPATH,'/html/body/form/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr[1]/td[1]/table/tbody/tr/td[3]/table/tbody/tr[3]/td/table/tbody/tr/td[2]/input' )
        inputBox.clear()
        inputBox.send_keys(query)
        inputBox.send_keys(Keys.RETURN)
        self.driver.implicitly_wait(self.timeToWait)

        colorBox = self.driver.find_element(By.XPATH, '/html/body/form/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr[1]/td[1]/table/tbody/tr/td[3]/table/tbody/tr[6]/td/div/select')
        colorLst = colorBox.text.split('\n')
        if len(colorLst) == 1:
            if colorLst[0] == '':
                return None
            index = 0
        elif len(colorLst) == 0:
            return None
        elif query in colorLst:
            index = colorLst.index(query)
        else:
            index = random.randint(0, len(colorLst)-1)

        select = Select(colorBox)
        try:
            select.select_by_index(index)
        except:
            return None
        selected_option = select.first_selected_option
        action = ActionChains(self.driver)
        action.double_click(selected_option).perform()
        self.driver.implicitly_wait(self.timeToWait)

        #rgb = self.driver.find_element(By.NAME, 'html_text').get_attribute('value')
        labBox = self.driver.find_element(By.XPATH, '/html/body/form/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr[3]/td[3]/table/tbody/tr[15]/td/table/tbody/tr')
        lab = labBox.find_elements(By.TAG_NAME, 'input')
        labLst = [float(i.get_attribute('value')) for i in lab]
        self.colorDict[tmpPkl] = labLst 
        return labLst
    
    def getCodeFromColor(self, color):
        rgb = [int(color[i:i+1], base=16) for i in range(0,6,2)]
        return rgb

    def delta_E_CMC(self, Lab1, Lab2, l=2, c=1):
        '''
        Referenced from 
        https://www.colour-science.org/api/0.3.3/html/_modules/colour/difference/delta_e.html
        '''
        L1, a1, b1 = Lab1
        L2, a2, b2 = Lab2

        c1 = np.sqrt(a1 * a1 + b1 * b1)
        c2 = np.sqrt(a2 * a2 + b2 * b2)
        sl = 0.511 if L1 < 16 else (0.040975 * L1) / (1 + 0.01765 * L1)
        sc = 0.0638 * c1 / (1 + 0.0131 * c1) + 0.638
        h1 = 0 if c1 < 0.000001 else (np.arctan2(b1, a1) * 180) / np.pi

        while h1 < 0:
            h1 += 360

        while h1 >= 360:
            h1 -= 360

        t = (0.56 + np.fabs(0.2 * np.cos((np.pi * (h1 + 168)) / 180))
            if 164 <= h1 <= 345 else
            0.36 + np.fabs(0.4 * np.cos((np.pi * (h1 + 35)) / 180)))
        c4 = c1 * c1 * c1 * c1
        f = np.sqrt(c4 / (c4 + 1900))
        sh = sc * (f * t + 1 - f)

        delta_L = L1 - L2
        delta_C = c1 - c2
        delta_A = a1 - a2
        delta_B = b1 - b2
        delta_H2 = delta_A * delta_A + delta_B * delta_B - delta_C * delta_C

        v1 = delta_L / (l * sl)
        v2 = delta_C / (c * sc)
        v3 = sh

        return np.sqrt(v1 * v1 + v2 * v2 + (delta_H2 / (v3 * v3)))
    
    def getCoSim(self, word1, word2):
        return np.dot(word1, word2) / (np.linalg.norm(word1) * np.linalg.norm(word2))
    


if __name__ == '__main__':
    testLost = {
            "lostBoardId" : 1,
            "productName" : "어린이 카드지갑",
            "color" : "검정",
            "categoryName" : "지갑",
            "description" : "물품에 대한 설명",
            "aiDescription": "",
            "lostAt" : "2024-03-26 00:00:00",
            "xpos" :127.04,
            "ypos" : 37.5013
        }
    testFound = [
            {
                "id": 959143,
                "depPlace": "을지지구대",
                "fdFilePathImg": "https://www.lost112.go.kr/lostnfs/images/uploadImg/20231114/20231114035402064.jpg",
                "fdPrdtNm": "지갑(카드 2개)",
                "fdSbjt": "지갑(카드 2개)(코발트(짙은청록)색)을 습득하여 보관하고 있습니다.",
                "clrNm": "짙은청록",
                "fdYmd": "2023-10-13",
                "mainPrdtClNm": "지갑"
            },
            {
                "id": 959144,
                "depPlace": "광희지구대",
                "fdFilePathImg": "https://www.lost112.go.kr/lostnfs/images/uploadImg/20231114/20231114035402064.jpg",
                "fdPrdtNm": "지갑(카드 2개)",
                "fdSbjt": "지갑(카드 2개)(코발트(짙은청록)색)을 습득하여 보관하고 있습니다.",
                "clrNm": "짙은청록",
                "fdYmd": "2023-10-13",
                "mainPrdtClNm": "지갑"
            },
            {
                "id": 959145,
                "depPlace": "약수",
                "fdFilePathImg": "https://www.lost112.go.kr/lostnfs/images/uploadImg/20231114/20231114035402064.jpg",
                "fdPrdtNm": "지갑(카드 2개)",
                "fdSbjt": "지갑(카드 2개)(코발트(짙은청록)색)을 습득하여 보관하고 있습니다.",
                "clrNm": "짙은청록",
                "fdYmd": "2023-10-13",
                "mainPrdtClNm": "지갑"
            }
        ]
    testModel = matchModel()
    testModel.setData(testLost, testFound)
    
    testModel.preprocess('lost112')
    # testModel.calColor()
    testModel.calDistance()
    testModel.calName()
    # testModel.calDesc()
    print(testModel.score)
    ans = testModel.aggregateScore()
    print(ans)