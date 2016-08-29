package com.zk.gooview.ui;

import com.zk.gooview.utils.GeometryUtil;
import com.zk.gooview.utils.Utils;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class GooView extends View {

	private Paint paint;
	public GooView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
	}

	public GooView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}

	public GooView(Context context) {
		this(context,null);
	}
	//拖拽圆的圆心和半径
	PointF mDragCenter = new PointF(80.0f, 80.0f);
	float mDragRadius = 16.0f;
	//固定圆的圆心和半径
	PointF mStickCenter = new PointF(150f,150f);
	float mStickRadius = 12.0f;

	PointF[] mStickPoints = new PointF[]{
			new PointF(250.0f, 100.0f),
			new PointF(250.0f, 300.0f)
	};
	PointF[] mDragPoints = new PointF[]{
			new PointF(50.0f,100.0f),
			new PointF(50.0f,300.0f)
	};
	PointF mControlPoint = new PointF(150.0f, 200.0f);
	private int statusBarHeight;

	@Override
	protected void onDraw(Canvas canvas) {
		//计算变量
		//计算固定圆的半径
		float tempStickRadius = computeStickRadius();
		//计算四个附着点的坐标
		float yOffset = mStickCenter.y - mDragCenter.y;
		float xOffset = mStickCenter.x - mDragCenter.x;

		Double lineK = null;
		if (xOffset != 0) {
			lineK = (double) (yOffset / xOffset);
		}
		//计算出拖拽圆的两个附着点坐标
		mDragPoints = GeometryUtil.getIntersectionPoints(mDragCenter, mDragRadius, lineK);
		mStickPoints = GeometryUtil.getIntersectionPoints(mStickCenter, tempStickRadius, lineK);
		//计算出控制点的坐标
		mControlPoint = GeometryUtil.getMiddlePoint(mDragCenter, mStickCenter);
		
		//移动画布
		canvas.save();//保存当前状态
		canvas.translate(0, -statusBarHeight);
		
//		//画出四个附着点
//		paint.setColor(Color.BLACK);
//		canvas.drawCircle(mStickPoints[0].x, mStickPoints[0].y, 3.0f, paint);
//		canvas.drawCircle(mStickPoints[1].x, mStickPoints[1].y, 3.0f, paint);
//		canvas.drawCircle(mDragPoints[0].x, mDragPoints[0].y, 3.0f, paint);
//		canvas.drawCircle(mDragPoints[1].x, mDragPoints[1].y, 3.0f, paint);
//		paint.setColor(Color.RED);
		//绘制最大范围的圆环
		paint.setStyle(Style.STROKE);
		canvas.drawCircle(mStickCenter.x, mStickCenter.y, farestDistance, paint);
		paint.setStyle(Style.FILL);
		if (!isDisappear) {
			
			if (!isOutOfRange) {
				//自定义路径
				Path path = new Path();
				//起始点
				path.moveTo(mStickPoints[0].x, mStickPoints[0].y);
				//从点1到点2  
				//x1 The x-coordinate of the control point on a quadratic curve
				//y1 The y-coordinate of the control point on a quadratic curve
				
				path.quadTo(mControlPoint.x, mControlPoint.y, mDragPoints[0].x, mDragPoints[0].y);
				path.lineTo(mDragPoints[1].x, mDragPoints[1].y);
				path.quadTo(mControlPoint.x, mControlPoint.y,mStickPoints[1].x, mStickPoints[1].y);
				//		path.cubicTo(x1, y1, x2, y2, x3, y3);三阶曲线
				path.close();
				
				canvas.drawPath(path, paint);
				//画一个固定圆
				canvas.drawCircle(mStickCenter.x, mStickCenter.y, tempStickRadius, paint);
			}
			
			//画一个拖拽圆
			canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, paint);
		}
		//恢复上一次保存的状态
		canvas.restore();
	}
	float farestDistance = 80.0f;
	private boolean isOutOfRange = false;//是否超出范围
	private boolean isDisappear = false;//是否消失
	private float computeStickRadius() {
		float distance = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
		distance = Math.min(distance, farestDistance);
		float percent = distance/farestDistance;
		return evaluate(percent, mStickRadius, mStickRadius*0.25);
	}
	 public Float evaluate(float fraction, Number startValue, Number endValue) {
	        float startFloat = startValue.floatValue();
	        return startFloat + fraction * (endValue.floatValue() - startFloat);
	    }
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		statusBarHeight = Utils.getStatusBarHeight(this);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x;
		float y;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			isOutOfRange = false;
			isDisappear = false; 
			
			x = event.getRawX();
			y = event.getRawY();
			updateDragCenter(x, y);
			break;
		case MotionEvent.ACTION_MOVE:
			x = event.getRawX();
			y = event.getRawY();
			updateDragCenter(x, y);
			
			//判断距离
			float d = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
			if (d > farestDistance) {
				isOutOfRange = true;
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
			if (isOutOfRange) {
				//刚刚超出范围
				float distance = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
				if (distance > farestDistance) {
					isDisappear  = true;
					invalidate();
				} else {
					//没有超出范围 更新坐标
					updateDragCenter(mStickCenter.x, mStickCenter.y);
				}
			} else {
				//刚刚没超出范围
				final PointF startPoint = new PointF(mDragCenter.x, mDragCenter.y);
				final PointF endPoint = mStickCenter;
				
				ValueAnimator animator = ValueAnimator.ofFloat(1.0f);
				animator.addUpdateListener(new AnimatorUpdateListener() {
					
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						float fraction = animation.getAnimatedFraction();
						PointF p = GeometryUtil.getPointByPercent(startPoint, endPoint, fraction);
						updateDragCenter(p.x, p.y);
					}
				});
				animator.setInterpolator(new OvershootInterpolator(4));
				animator.setDuration(500);
				animator.start();
			}
			break;

		default:
			break;
		}

		return true;
	}
	/**
	 * 更新拖拽圆圆心坐标，重绘界面
	 * @param x
	 * @param y
	 */
	public void updateDragCenter(float x, float y) {
		mDragCenter.set(x, y);
		invalidate();
	}

}
