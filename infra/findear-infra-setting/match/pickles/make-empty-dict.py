import pickle

colorDict = dict()
with open('colorDict.pickle', 'wb') as f:
    pickle.dump(colorDict, f)
