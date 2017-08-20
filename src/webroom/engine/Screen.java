package webroom.engine;

import java.util.ArrayList;

public class Screen {

    private final int[][] map;
    private int width, height;
    private final ArrayList<Texture> textures;
    private final Texture floor;
    private final Texture ceiling;
    private ArrayList<Sprite> systemSprites;
    private ArrayList<Sprite> userSprites;
    public Screen(int[][] m, ArrayList<Texture> tex, int w, int h, Texture f, Texture c,  ArrayList<Sprite> sprites,ArrayList<Sprite> usersprites) {
        // inverse x and y as calculus are made for y,x
        map = m;
        textures = tex;
        width = w;
        height = h;
        floor = f;
        ceiling = c;
        this.systemSprites = sprites;
    this.userSprites = usersprites;
    }   

    public int[] update(Camera camera, int[] pixels) {
        double[] ZBuffer = new double[width];
        for (int x = 0; x < width; x = x + 1) {
            double cameraX = 2 * x / (double) (width) - 1;
            double rayDirX = camera.xDir + camera.xPlane * cameraX;
            double rayDirY = camera.yDir + camera.yPlane * cameraX;
            //Map position
            int mapX = (int) camera.xPos;
            int mapY = (int) camera.yPos;
            //length of ray from current position to next x or y-side
            double sideDistX;
            double sideDistY;
            //Length of ray from one side to next in map
            double deltaDistX = Math.sqrt(1 + (rayDirY * rayDirY) / (rayDirX * rayDirX));
            double deltaDistY = Math.sqrt(1 + (rayDirX * rayDirX) / (rayDirY * rayDirY));
            double perpWallDist;
            //Direction to go in x and y
            int stepX, stepY;
            boolean hit = false;//was a wall hit
            int side = 0;//was the wall vertical or horizontal
            //Figure out the step direction and initial distance to a side
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (camera.xPos - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - camera.xPos) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (camera.yPos - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - camera.yPos) * deltaDistY;
            }
            //Loop to find where the ray hits a wall
            while (!hit) {
                //Jump to next square
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                //Check if ray has hit a wall
                //System.out.println(mapX + ", " + mapY + ", " + map[mapX][mapY]);
                if (map[mapX][mapY] > 0) {
                    hit = true;
                }
            }
            //Calculate distance to the point of impact
            if (side == 0) {
                perpWallDist = Math.abs((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX);
            } else {
                perpWallDist = Math.abs((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY);
            }
            //Now calculate the height of the wall based on the distance from the camera
            int lineHeight;
            if (perpWallDist > 0) {
                lineHeight = Math.abs((int) (height / perpWallDist));
            } else {
                lineHeight = height;
            }
            //calculate lowest and highest pixel to fill in current stripe
            int drawStart = -lineHeight / 2 + height / 2;
            if (drawStart < 0) {
                drawStart = 0;
            }
            int drawEnd = lineHeight / 2 + height / 2;
            if (drawEnd >= height) {
                drawEnd = height - 1;
            }
            //add a texture
            int texNum = map[mapX][mapY] - 1;
            double wallX;//Exact position of where wall was hit
            if (side == 1) {//If its a y-axis wall
                wallX = (camera.xPos + ((mapY - camera.yPos + (1 - stepY) / 2) / rayDirY) * rayDirX);
            } else {//X-axis wall
                wallX = (camera.yPos + ((mapX - camera.xPos + (1 - stepX) / 2) / rayDirX) * rayDirY);
            }
            wallX -= Math.floor(wallX);
            //x coordinate on the texture
            int texX = (int) (wallX * (textures.get(texNum).SIZE));
            if (side == 0 && rayDirX > 0) {
                texX = textures.get(texNum).SIZE - texX - 1;
            }
            if (side == 1 && rayDirY < 0) {
                texX = textures.get(texNum).SIZE - texX - 1;
            }
            //calculate y coordinate on texture
            for (int y = drawStart; y < drawEnd; y++) {
                int texY = (((y * 2 - height + lineHeight) << 8) / lineHeight) / 1;
                int color = textures.get(texNum).pixels[texX + (texY * textures.get(texNum).SIZE)];
                int alpha = (lineHeight * 255 / Texture.SIZE)+50;
                if (alpha > 255) alpha = 255;
                pixels[x + y * (width)] = color & ((alpha<<24)|0xFFFFFF);
            }
            ZBuffer[x] = perpWallDist; //perpendicular distance is used
            //FLOOR CASTING
            double floorXWall, floorYWall; //x, y position of the floor texel at the bottom of the wall

            //4 different wall directions possible
            if (side == 0 && rayDirX > 0) {
                floorXWall = mapX;
                floorYWall = mapY + wallX;
            } else if (side == 0 && rayDirX < 0) {
                floorXWall = mapX + 1.0;
                floorYWall = mapY + wallX;
            } else if (side == 1 && rayDirY > 0) {
                floorXWall = mapX + wallX;
                floorYWall = mapY;
            } else {
                floorXWall = mapX + wallX;
                floorYWall = mapY + 1.0;
            }

            double distWall, currentDist;

            distWall = perpWallDist;
            if (drawEnd < 0) {
                drawEnd = height; //becomes < 0 when the integer overflows
            }
            //draw the floor from drawEnd to the bottom of the screen
            for (int y = drawEnd + 1; y < height; y++) {
                currentDist = height / (2.0 * y - height); //you could make a small lookup table for this instead
                double weight = (currentDist) / (distWall);
                double currentFloorX = weight * floorXWall + (1.0 - weight) * camera.xPos;
                double currentFloorY = weight * floorYWall + (1.0 - weight) * camera.yPos;

                int floorTexX, floorTexY;
                floorTexX = (int) (currentFloorX * floor.SIZE) % floor.SIZE;
                floorTexY = (int) (currentFloorY * floor.SIZE) % floor.SIZE;
                //ceiling (symmetrical!)
                int alpha = (int)((y-(height/2)) * 255 / (height/2))+50;
                if (alpha > 255) alpha = 255;
                if (alpha < 0) alpha = 0;
                int ceilingColor = ceiling.pixels[(ceiling.SIZE * floorTexY) + floorTexX];
                pixels[x + ((height - y) * width)] = ceilingColor & ((alpha<<24)|0xFFFFFF);
                //floor
                int floorColor = floor.pixels[floor.SIZE * floorTexY + floorTexX];
                pixels[x + (y * width)] = floorColor & ((alpha<<24)|0xFFFFFF);
            }

        }
        //SPRITE CASTING

        //sort sprites from far to close
        ArrayList<Sprite> sprites = new ArrayList<>();
        sprites.addAll(systemSprites);
        sprites.addAll(userSprites);
        for (int i = 0; i < sprites.size(); i++) {
            sprites.get(i).distance = ((camera.xPos - sprites.get(i).y) * (camera.xPos - sprites.get(i).y) + (camera.yPos - sprites.get(i).x) * (camera.yPos - sprites.get(i).x)); //sqrt not taken, unneeded
        }
        //combSort(spriteOrder, spriteDistance, sprites.size());
        sprites.sort(new java.util.Comparator<Sprite>() {
            @Override
            public int compare(Sprite o1, Sprite o2) {
                return new Double(o2.distance).compareTo(new Double(o1.distance));
            }
        });

        //after sorting the sprites, do the projection and draw them
        for (int i = 0; i < sprites.size(); i++) {
            //translate sprite position to relative to camera
            // coordinates are expecting y,y
            double spriteX = sprites.get(i).y - camera.xPos;
            double spriteY = sprites.get(i).x - camera.yPos;
            double invDet = 1.0 / (camera.xPlane * camera.yDir - camera.xDir * camera.yPlane); //required for correct matrix multiplication
            double transformX = invDet * (camera.yDir * spriteX - camera.xDir * spriteY);
            double transformY = invDet * (-camera.yPlane * spriteX + camera.xPlane * spriteY); //this is actually the depth inside the screen, that what Z is in 3D
            int spriteScreenX = (int) ((width / 2) * (1 + transformX / transformY));

            //calculate height of the sprite on screen
            int spriteHeight = Math.abs((int) (height / (transformY))); //using "transformY" instead of the real distance prevents fisheye

            int drawStartY = -spriteHeight / 2 + height / 2;
            if (drawStartY < 0) {
                drawStartY = 0;
            }
            int drawEndY = spriteHeight / 2 + height / 2;
            if (drawEndY >= height) {
                drawEndY = height - 1;
            }

            //calculate width of the sprite
            int spriteWidth = Math.abs((int) (height / (transformY)));
            int drawStartX = -spriteWidth / 2 + spriteScreenX;
            if (drawStartX < 0) {
                drawStartX = 0;
            }
            int drawEndX = spriteWidth / 2 + spriteScreenX;
            if (drawEndX >= width) {
                drawEndX = width - 1;
            }
            int alpha = (spriteHeight * 255 / height)+50;
            if (alpha > 255) alpha = 255;
            //loop through every vertical stripe of the sprite on screen
            for (int stripe = drawStartX; stripe < drawEndX; stripe++) {
                int texX = (int) (256 * (stripe - (-spriteWidth / 2 + spriteScreenX)) * Texture.SIZE / spriteWidth) / 256;
                if (transformY > 0 && stripe > 0 && stripe < width && transformY < ZBuffer[stripe]) {
                    for (int y = drawStartY; y < drawEndY; y++) //for every pixel of the current stripe
                    {
                        int d = (y) * 256 - height * 128 + spriteHeight * 128; //256 and 128 factors to avoid floats
                        int texY = ((d * Texture.SIZE) / spriteHeight) / 256;

                        int color = sprites.get(i).texture.pixels[(Texture.SIZE * texY) + texX]; //get current color from the texture
                        if ((color & 0x00FFFFFF) != 0) {
                            pixels[(y * width) + stripe] = color & ((alpha<<24) | 0xFFFFFF); //paint pixel if it isn't black, black is the invisible color
                        }
                    }
                }
            }

        }
        return pixels;
    }
}
