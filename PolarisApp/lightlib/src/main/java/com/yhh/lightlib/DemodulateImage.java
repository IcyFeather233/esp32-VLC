package com.yhh.lightlib;

import java.util.ArrayList;

public class DemodulateImage {
	
	private static int GsTH=0;
	private static int [] HeaderIdx=new int[20];
	private static ArrayList<Character> GsHorL = new ArrayList<Character>();
	private static ArrayList<Integer> GsWidth = new ArrayList<Integer>();
	private static ArrayList<Integer> GsIndex= new ArrayList<Integer>();
	private static ArrayList<Integer> GsValueF= new ArrayList<Integer>();//存储用过滤值处理过的数据
	private static ArrayList<Integer> HDidx= new ArrayList<Integer>();
	private static ArrayList<Integer> GsHDidx= new ArrayList<Integer>();


	private static int [] DemID1p=new int [20];
	private static int [] DemID2p=new int [20];
	private static int DemID1pcnt=0;
	private static int DemID2pcnt=0;
	static int[] DemIDTemp=new int[2];
	static int ReDemod=1;  //重复解调计数
	static boolean StopDemod=false;

	private static int filterws=3;//过滤器
	static double HeaderGsTH= 0.57;
	static double SymbolWidTH= 0.8;

	static int DataSize=20;


	public static int DemodImage( ArrayList<Integer> GsIn)
	{  
		int DemID=0;//存储计算出来的id值
		int [] GsMaxMinT= new int[2];




		int maxgsL=0;  //For OCC  数组的左侧（范围是数组的1/20）的最大平均灰度值
		int maxgsLidx=0;  //For OCC 这个值在数组的位置
		int maxgsM=0;  //For OCC   平均灰度值数组的最大值
		int maxgsMidx=0;  //For OCC  该最大值在数组里的位置
		int maxgsR=0;  //For OCC  数组的右侧（范围是数组的19/20）的最大平均灰度值
		int maxgsRidx=0;  //For OCC 这个值在数组的位置


		int mings=200; //数组最小值


		int EdgeOffset=GsIn.size()/20;//表示边界的偏移量为数组大小的1/20
		int grayscale=0;

		GsValueF.clear();
		for(int j=0;j<GsIn.size();j++)
		{

			int tempValue=0;
			if(j>filterws)
			{

				for(int i=0;i<filterws;i++)
				{
					tempValue= tempValue+ GsIn.get(j-i);
				}

				tempValue=tempValue/filterws;//取平均值

				GsValueF.add(tempValue);//存储计算出来的平均值
			}
		}


		for(int l=0; l<GsValueF.size(); l++)//依次读取平均灰度值数组的值
		{

			grayscale=GsValueF.get(l);

			if(grayscale<mings)//更新最小的平均灰度值
			{
				mings=grayscale;
			}

			if(grayscale>maxgsM)//更新最大的平均灰度值
			{
				maxgsMidx=l;//记录这个值在GsValueF的位置
				maxgsM=grayscale;
			}

			if(l<EdgeOffset)   //数组左侧
			{
				if(grayscale>maxgsL)
				{
					maxgsLidx=l;//记录这个值在数组的位置
					maxgsL=grayscale;//左侧的最大平均灰度值
				}
			}

			if(l>GsValueF.size()-EdgeOffset)   //数组右侧
			{
				if(grayscale>maxgsR)
				{
					maxgsRidx=l;//记录这个值在数组的位置
					maxgsR=grayscale;//右侧的最大平均灰度值
				}
			}
		}

		double headerk1 = 0,headerk2 = 0;

		if(maxgsLidx<maxgsMidx &&maxgsMidx<maxgsRidx)
		{
			headerk1=(double) (maxgsM-maxgsL)/(maxgsMidx-maxgsLidx);
			headerk2=(double) (maxgsM-maxgsR)/(maxgsMidx-maxgsRidx);
		}else if(maxgsLidx==maxgsMidx &&maxgsMidx<maxgsRidx)
		{
			headerk1=0;
			headerk2=(double) (maxgsM-maxgsR)/(maxgsMidx-maxgsRidx);
		}else if(maxgsLidx<maxgsMidx &&maxgsMidx==maxgsRidx)
		{
			headerk1=(double) (maxgsM-maxgsL)/(maxgsMidx-maxgsLidx);
			headerk2=0;
		}

		GsHorL.clear();
		GsWidth.clear();
		GsIndex.clear();
		int Hwidcnt=0;
		int Lwidcnt=0;
		boolean HWriteFlag=true;
		boolean LWriteFlag=true;
		double thforheader_gray;


		for(int ii=0; ii<  GsValueF.size();ii++) {
			if (ii < maxgsMidx)
				thforheader_gray = (double) HeaderGsTH * ((maxgsM - headerk1 * (maxgsMidx - ii)) + mings);
			else
				thforheader_gray = (double) HeaderGsTH * ((maxgsM + headerk2 * (ii - maxgsMidx)) + mings);

			//OOK
			if(GsValueF.get(ii)>thforheader_gray)
			{
				Hwidcnt++;
				if(LWriteFlag)
				{
					LWriteFlag=false;
					GsHorL.add('L');
					GsWidth.add(Lwidcnt);
					GsIndex.add(ii-Lwidcnt/2);


				}
				Lwidcnt=0;
				HWriteFlag=true;

			}else
			{
				Lwidcnt++;
				if(HWriteFlag )
				{
					HWriteFlag=false;
					GsHorL.add('H');
					GsWidth.add(Hwidcnt);
					GsIndex.add(ii-Hwidcnt/2);


				}
				LWriteFlag=true;
				Hwidcnt=0;
			}

		}


		int [] WdMaxMinT= new int[2];
		WdMaxMinT=FindMaxMin(GsWidth);//查找灯带的最大和最小宽度，以设置标头的阈值。

		System.out.println("Maxband="+WdMaxMinT[0]+",Minband="+WdMaxMinT[1]);

		int HeaderBandTH=WdMaxMinT[0]-WdMaxMinT[1];

		HDidx.clear();
		GsHDidx.clear();
		if(GsWidth.size()>18)
		{
			for(int i=0; i<GsWidth.size();i++)
			{
				if(GsWidth.get(i)>HeaderBandTH)
				{
					HDidx.add(i);
					GsHDidx.add(GsIndex.get(i));
				}
			}

		}


		if(HDidx.size()==1)
		{


		}else if(HDidx.size()>1)
		{

			DemID=DemodulateWith2Headers(HDidx);

		}

		return DemID;
	}

	/***
	 * 使用两个以上的标头从帧中解调数据
	 * @param //ALIn
	 * @return
	 */
	private static int DemodulateWith2Headers(ArrayList<Integer> ALin)
	{

		int LightIDt = 0;

		System.out.println("ALin.size()="+ALin.size());

		double OverSR=0;
		double SymbolTH=0;

		for(int i=1;i<ALin.size();i++)
		{
			OverSR= (GsHDidx.get(i)-GsHDidx.get(i-1))/DataSize;

			SymbolTH=OverSR*SymbolWidTH;

			String DemodOut="";

			for(int k=ALin.get(i-1)+1;k<ALin.get(i);k++)
			{
				if(GsHorL.get(k)=='H')
				{

					if(GsWidth.get(k)<2*SymbolTH)
					{
						DemodOut=DemodOut+"1";

					}else
					{
						DemodOut=DemodOut+"11";
					}
				}else
				{
					if(GsWidth.get(k)<2*SymbolTH)
					{
						DemodOut=DemodOut+"0";

					}else
					{
						DemodOut=DemodOut+"00";
					}
				}
			}

			System.out.println(" DemodOut.length()="+DemodOut.length()+"-"+DemodOut);
			if(DemodOut.length()==18)
			{
				LightIDt=ConvertStrToID(DemodOut);
			}
		}

		return  LightIDt;
	}



	/*
	* 解调 FSK 符号并解码曼彻斯特代码
    */
	public static int ConvertStrToID(String str)
	{
		int[] iID=new int[16];
		int[] iIDt=new int[8];
		int[] demodID=new int[2];
		int DmIDt=0;
		
		demodID[0]= 0;
		demodID[1]= 0;
	
			if(str.substring(17,18).equals("0"))
			{

				for(int j=1;j<9;j++)
				{
					System.out.print("-"+str.substring(2*j-1,2*j+1));

					if(str.substring(2*j-1,2*j+1).equals("01"))
					{
						iIDt[j-1]=0;
					}else
					{
						iIDt[j-1]=1;
					}
				}
				
				demodID[0]=(2*iIDt[0]+iIDt[1]);
				demodID[1]=(32*iIDt[2]+16*iIDt[3]+8*iIDt[4]+4*iIDt[5]+2*iIDt[6]+iIDt[7]);
				if(demodID[0]==0)
				{
					DemID1p[DemID1pcnt]=demodID[1];
					DemID1pcnt++;
				}
				if(demodID[0]==1)
				{
					DemID2p[DemID2pcnt]=demodID[1];
					DemID2pcnt++;
				}
				if(DemID1pcnt>ReDemod&& DemID2pcnt>ReDemod)
				{
					if(DemID1pcnt!=DemID2pcnt)
					{
						if(DemID1pcnt>DemID2pcnt)
							DemID1pcnt=DemID2pcnt;
					}
					if(DemID1p[0]==DemID1p[1] && DemID2p[0]==DemID2p[1] && (!StopDemod))
					{
						DmIDt=64*DemID1p[0]+DemID2p[0];
						StopDemod=true;
					}
					DemID1pcnt=0;
					DemID2pcnt=0;
				}
		  }
		return DmIDt;
	}

	/***
	 * @param ALin
	 * @return
	 */
	private static int[] FindMaxMin(ArrayList<Integer> ALin)
	{
		int[] result=new int[2];
		
		    result[0] = 0;
	        result[1] = 100;
	        for (int i = 1; i < ALin.size(); i++) 
	        {
	            if (ALin.get(i) > result[0]) {
	                result[0] = ALin.get(i);
	            }

	            if (ALin.get(i) < result[1]) {
	                result[1] = ALin.get(i);
	            }
	        }
		
		return result;
	}
	
	
}
