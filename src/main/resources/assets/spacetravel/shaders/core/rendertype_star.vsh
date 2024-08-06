#version 150

in vec3 Position;
in vec4 Color;
in vec3 StarPos;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 RelativeSpacePos;

float DEFAULT_DISTANCE = 100;
mat4 matrix = mat4(	1.0, 0.0, 0.0, 0.0,
					0.0, 1.0, 0.0, 0.0,
					0.0, 0.0, 1.0, 0.0,
					0.0, 0.0, 0.0, 1.0);

out vec4 vertexColor;

float clampStar(float starSize, float distance)
{
	float maxStarSize = 0.2 + starSize / 5;
	starSize = starSize * 200000.0 * distance;
	
	if(starSize < 0.05)
		return 0.05;
	
	return starSize > maxStarSize ? maxStarSize : starSize;
}

void main() {
	float x = StarPos.x - RelativeSpacePos.x;
	float y = StarPos.y - RelativeSpacePos.y;
	float z = StarPos.z - RelativeSpacePos.z;
	
	float distance = x * x + y * y + z * z;
	
	// COLOR START
	
	float alpha = Color.w;
	float minAlpha = (alpha - 0.66) * 2 / 3;
	
	if(alpha < minAlpha)
			alpha = minAlpha;
	
	// COLOR END
	
	distance = 1.0 / sqrt(distance);
	x *= distance;
	y *= distance;
	z *= distance;
	
	// This effectively pushes the Star away from the camera
	// It's better to have them very far away, otherwise they will appear as though they're shaking when the Player is walking
	float starX = x * DEFAULT_DISTANCE;
	float starY = y * DEFAULT_DISTANCE;
	float starZ = z * DEFAULT_DISTANCE;
	
	float starSize = clampStar(Position.z, distance);
	
	/* These very obviously represent Spherical Coordinates (r, theta, phi)
	 * 
	 * Spherical equations (adjusted for Minecraft, since usually +Z is up, while in Minecraft +Y is up):
	 * 
	 * r = sqrt(x * x + y * y + z * z)
	 * tetha = arctg(x / z)
	 * phi = arccos(y / r)
	 * 
	 * x = r * sin(phi) * sin(theta)
	 * y = r * cos(phi)
	 * z = r * sin(phi) * cos(theta)
	 * 
	 * Polar equations
	 * z = r * cos(theta)
	 * x = r * sin(theta)
	 */
	float sphericalTheta = atan(x, z);
	float sinTheta = sin(sphericalTheta);
	float cosTheta = cos(sphericalTheta);
	
	float xzLength = sqrt(x * x + z * z);
	float sphericalPhi = atan(xzLength, y);
	float sinPhi = sin(sphericalPhi); //TODO These don't repeat so remove them
	float cosPhi = cos(sphericalPhi); //
	
	float height = Position.x * starSize;
	float width = Position.y * starSize;
	
	float heightProjectionY = height * sinPhi;
	
	float heightProjectionXZ = - height * cosPhi;
	
	/* 
	 * projectedX:
	 * Projected height is projected onto the X-axis using sin(theta) and then gets subtracted (added because it's already negative)
	 * Width is projected onto the X-axis using cos(theta) and then gets subtracted
	 * 
	 * projectedZ:
	 * Width is projected onto the Z-axis using sin(theta)
	 * Projected height is projected onto the Z-axis using cos(theta) and then gets subtracted (added because it's already negative)
	 * 
	 */
	float projectedX = heightProjectionXZ * sinTheta - width * cosTheta;
	float projectedZ = width * sinTheta + heightProjectionXZ * cosTheta;
	
	vec3 pos = vec3(projectedX + starX, heightProjectionY + starY, projectedZ + starZ);
	
	gl_Position = ProjMat * ModelViewMat * matrix * vec4(pos, 1.0);
	
	vertexColor = vec4(Color.x, Color.y, Color.z, alpha);
}
