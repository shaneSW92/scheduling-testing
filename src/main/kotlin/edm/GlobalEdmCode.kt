package edm

enum class GlobalEdmCode (type: EDM) {
    RG (Earning.NORMAL),
    HOL (Earning.HOLIDAY),
    OT (Earning.OVERTIME),
    DBLOT (Earning.OVERTIME),
    BRK (Memo.MEMO),
    PABRK (Earning.NORMAL),
    PBRK (Earning.NORMAL),
    T (Earning.TRAINING),
    TR (Earning.TRAINING)
}